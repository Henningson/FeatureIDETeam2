/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2013  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 * 
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://www.fosd.de/featureide/ for further information.
 */
package de.ovgu.featureide.featurehouse;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.eclipse.jdt.internal.core.JavaProject;

import cide.gparser.ParseException;
import cide.gparser.TokenMgrError;

import composer.CmdLineInterpreter;
import composer.FSTGenComposer;
import composer.FSTGenComposerExtension;
import composer.IParseErrorListener;

import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.builder.ComposerExtensionClass;
import de.ovgu.featureide.featurehouse.errorpropagation.ErrorPropagation;
import de.ovgu.featureide.featurehouse.meta.FeatureModelClassGenerator;
import de.ovgu.featureide.featurehouse.meta.VerifyProductAction;
import de.ovgu.featureide.featurehouse.model.FeatureHouseModelBuilder;
import de.ovgu.featureide.fm.core.FMCorePlugin;
import de.ovgu.featureide.fm.core.Feature;
import de.ovgu.featureide.fm.core.FeatureModel;
import de.ovgu.featureide.fm.core.configuration.Configuration;

/**
 * Composes files using FeatureHouse.
 * 
 * @author Tom Brosch
 */
// TODO set "Composition errors" like *.png could not be composed with *.png
@SuppressWarnings("restriction")
public class FeatureHouseComposer extends ComposerExtensionClass {

	private static final String CONTRACT_COMPOSITION_CONSECUTIVE_CONTRACT_REFINEMENT = "consecutive contract refinement";
	private static final String CONTRACT_COMPOSITION_EXPLICIT_CONTRACT_REFINEMENT = "explicit contract refinement";
	private static final String CONTRACT_COMPOSITION_CONTRACT_OVERRIDING = "contract overriding";
	private static final String CONTRACT_COMPOSITION_PLAIN_CONTRACTING = "plain contracting";
	private static final String CONTRACT_COMPOSITION_EXPLICIT_CONTRACTING = "explicit_contracting";
	private static final String CONTRACT_COMPOSITION_CONSECUTIVE_CONTRACTING = "consecutive_contracting";
	private static final String CONTRACT_COMPOSITION_NONE = "none";

	public static final String COMPOSER_ID = "de.ovgu.featureide.composer.featurehouse";

	
	private FSTGenComposer composer;

	public FeatureHouseModelBuilder fhModelBuilder;
	
	private ErrorPropagation errorPropagation = new ErrorPropagation();

	private IParseErrorListener listener = createParseErrorListener();

	private IParseErrorListener createParseErrorListener() {
		return new IParseErrorListener() {

			@Override
			public void parseErrorOccured(ParseException e) {
				createBuilderProblemMarker(e.currentToken.next.endLine, e.getMessage());
			}
		};
	}

	@Override
	public boolean initialize(IFeatureProject project) {
		boolean supSuccess =super.initialize(project);
		fhModelBuilder = new FeatureHouseModelBuilder(project);
		createBuildStructure();
		if(supSuccess==false || fhModelBuilder==null) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Creates an error marker to the last error file.
	 * @param line The line of the marker.
	 * @param message The message.
	 */
	protected void createBuilderProblemMarker(int line, String message) {
		message = detruncateString(message);
		try {
			IMarker marker = getErrorFile().createMarker(FeatureHouseCorePlugin.BUILDER_PROBLEM_MARKER);
			marker.setAttribute(IMarker.LINE_NUMBER, line);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
		} catch (CoreException e) {
			FeatureHouseCorePlugin.getDefault().logError(e);
		}

	}

	/**
	 * @param message
	 *            The message
	 * @return A substring of message that is smaller than 65535 bytes.
	 */
	private static String detruncateString(String message) {
		byte[] bytes;
		try {
			bytes = message.getBytes(("UTF-8"));
			if (bytes.length > 65535)
				message = message.substring(0, 65535 / 2);

		} catch (UnsupportedEncodingException e) {
			FeatureHouseCorePlugin.getDefault().logError(e);
		}
		return message;
	}

	/**
	 * Gets the file containing the actual error.
	 * @return The file.
	 */
	protected IFile getErrorFile() {
		return featureProject.getProject().getWorkspace().getRoot().findFilesForLocationURI(
						composer.getErrorFiles().getLast().toURI())[0];
	}
	
	/**
	 * Removes line and column form the message of the TokenMgrError.<br>
	 * Example message:<br>
	 * -Lexical error at line 7, column 7.  Encountered: <EOF> after : "" 
	 * @param message The message
	 * @return message without "line i, column j." 
	 */
	private String getTokenMgrErrorMessage(String message) {
		if (!message.contains("line ") || !message.contains("Encountered")) return message;
		return message.substring(0, message.indexOf(" at ")) + " e" + message.substring(message.indexOf("ncountered:"));
	}
	
	/**
	 * Gets the line of the message of the TokenMgrError.<br>
	 * Example message:<br>
	 * -Lexical error at line 7, column 7.  Encountered: <EOF> after : ""
	 * @param message The error message
	 * @return The line
	 */
	private int getTokenMgrErrorLine(String message) {
		if (!message.contains("line ")) return -1;
		return Integer.parseInt(message.substring(message.indexOf("line ") + 5, message.indexOf(',')));
	}

	/**
	 * Checks the current folder structure at the build folder and creates folders if necessary.
	 */
	private void createBuildStructure() {
		IProject p = featureProject.getProject();
		if (p != null) {
			IFolder sourcefolder = featureProject.getBuildFolder();
			if (sourcefolder != null) {
				if (!sourcefolder.exists()) {
					try {
						sourcefolder.create(true, true, null);
					} catch (CoreException e) {
						FeatureHouseCorePlugin.getDefault().logError(e);
					}
				}
				IFile conf = featureProject.getCurrentConfiguration();
				if (conf != null) {
					String configName = conf.getName();
					sourcefolder = sourcefolder.getFolder(configName.substring(0, configName.indexOf('.')));
					if (!sourcefolder.exists()) {
						try {
							sourcefolder.create(true, true, null);
						} catch (CoreException e) {
							FeatureHouseCorePlugin.getDefault().logError(e);
						}
						callCompiler();
					}
					//setJavaBuildPath(conf.getName().split("[.]")[0]);
				}
			}
		}
	}

	// TODO refactor into cases
	public void performFullBuild(IFile config) {
		assert (featureProject != null) : "Invalid project given";
		final String configPath = config.getRawLocation().toOSString();
		final String basePath = featureProject.getSourcePath();
		final String outputPath = featureProject.getBuildPath();

		if (configPath == null || basePath == null || outputPath == null)
			return;

		IFolder buildFolder = featureProject.getBuildFolder().getFolder(
				config.getName().split("[.]")[0]);
		if (!buildFolder.exists()) {
			try {
				buildFolder.create(true, true, null);
				buildFolder.refreshLocal(IResource.DEPTH_ZERO, null);
			} catch (CoreException e) {
				FeatureHouseCorePlugin.getDefault().logError(e);
			}
		}
		
		setJavaBuildPath(config.getName().split("[.]")[0]);
		if (buildMetaProduct()) {
			new FeatureModelClassGenerator(featureProject);
			FSTGenComposerExtension.key = IFeatureProject.DEFAULT_META_PRODUCT_GENERATION.equals(featureProject.getMetaProductGeneration());
			composer = new FSTGenComposerExtension();
			FeatureModel featureModel = featureProject.getFeatureModel();
			List<String> featureOrderList = featureModel.getConcreteFeatureNames();
			// dead features should not be composed
			LinkedList<String> deadFeatures = new LinkedList<String>();
			for (Feature deadFeature : featureModel.getAnalyser().getDeadFeatures()) {
				deadFeatures.add(deadFeature.getName());
			}
			
			String[] features = new String[featureOrderList.size()];
			int i = 0;		
			for (String f : featureOrderList) {
				if (!deadFeatures.contains(f)) {
					features[i++] = f;
				}
			}
			
			try {
				((FSTGenComposerExtension) composer).buildMetaProduct(new String[] {
						CmdLineInterpreter.INPUT_OPTION_EQUATIONFILE, configPath,
						CmdLineInterpreter.INPUT_OPTION_BASE_DIRECTORY, basePath,
						CmdLineInterpreter.INPUT_OPTION_OUTPUT_DIRECTORY, outputPath + "/",
						CmdLineInterpreter.INPUT_OPTION_CONTRACT_STYLE, CONTRACT_COMPOSITION_EXPLICIT_CONTRACTING
				}, features);
				fhModelBuilder.buildModel(composer.getFstnodes(), false);
			} catch (TokenMgrError e) {
			} catch (Error e) {
				FeatureHouseCorePlugin.getDefault().logError(e);
			}
		} else {
			composer = new FSTGenComposer(false);
			try {
				composer.run(new String[] {
						CmdLineInterpreter.INPUT_OPTION_EQUATIONFILE, configPath,
						CmdLineInterpreter.INPUT_OPTION_BASE_DIRECTORY, basePath,
						CmdLineInterpreter.INPUT_OPTION_OUTPUT_DIRECTORY, outputPath + "/",
						CmdLineInterpreter.INPUT_OPTION_CONTRACT_STYLE, getContractParameter()
				});
			} catch (TokenMgrError e) {
				
			}
			fhModelBuilder.buildModel(composer.getFstnodes(), false);
		}
		if (verifyProduct()) {
			(new VerifyProductAction()).runMonkey(featureProject.getProject());
		}
		
		composer = new FSTGenComposerExtension();
		composer.addParseErrorListener(listener);
		
		List<String> featureOrderList = featureProject.getFeatureModel().getConcreteFeatureNames();
		String[] features = new String[featureOrderList.size()];
		int i = 0;
		for (String f : featureOrderList) {
			features[i++] = f;
		}
		
		try {
			((FSTGenComposerExtension)composer).buildFullFST(new String[] {
					CmdLineInterpreter.INPUT_OPTION_EQUATIONFILE, configPath,
					CmdLineInterpreter.INPUT_OPTION_BASE_DIRECTORY, basePath,
					CmdLineInterpreter.INPUT_OPTION_OUTPUT_DIRECTORY, outputPath + "/", 
					CmdLineInterpreter.INPUT_OPTION_CONTRACT_STYLE, getContractParameter()
			}, features);
		} catch (TokenMgrError e) {
			createBuilderProblemMarker(getTokenMgrErrorLine(e.getMessage()), getTokenMgrErrorMessage(e.getMessage()));
		} catch (Error e) {
			FeatureHouseCorePlugin.getDefault().logError(e);
		}
		ArrayList<FSTNode> fstnodes = composer.getFstnodes();
		if (fstnodes != null) {
			fhModelBuilder.buildModel(fstnodes, true);
			TreeBuilderFeatureHouse fstparser = new TreeBuilderFeatureHouse(
					featureProject.getProjectName());
			fstparser.createProjectTree(fstnodes);
			featureProject.setProjectTree(fstparser.getProjectTree());
		}
		callCompiler();
	}

	private String getContractParameter() {
		String contractComposition= featureProject.getContractComposition().toLowerCase(Locale.ENGLISH);
		if(CONTRACT_COMPOSITION_NONE.equals(contractComposition)) {
			return CONTRACT_COMPOSITION_NONE;
		}
		if(CONTRACT_COMPOSITION_PLAIN_CONTRACTING.equals(contractComposition)) {
			return CONTRACT_COMPOSITION_PLAIN_CONTRACTING;
		}
		if(CONTRACT_COMPOSITION_CONTRACT_OVERRIDING.equals(contractComposition)) {
			return CONTRACT_COMPOSITION_CONTRACT_OVERRIDING;
		}
		if(CONTRACT_COMPOSITION_EXPLICIT_CONTRACT_REFINEMENT.equals(contractComposition)) {
			return CONTRACT_COMPOSITION_EXPLICIT_CONTRACTING;
		}
		if(CONTRACT_COMPOSITION_CONSECUTIVE_CONTRACT_REFINEMENT.equals(contractComposition)) {
			return CONTRACT_COMPOSITION_CONSECUTIVE_CONTRACTING;
		}
		return CONTRACT_COMPOSITION_NONE;
	}

	/**
	 * This job calls the compiler by touching the .classpath file of the project.<br> 
	 * This is necessary after calling <code>setAsCurrentConfiguration</code>.
	 */
	private void callCompiler() {
		Job job = new Job("Call compiler") {
			protected IStatus run(IProgressMonitor monitor) {
				IFile iClasspathFile = featureProject.getProject()
						.getFile(".classpath");
				if (iClasspathFile.exists()) {
					try {
						iClasspathFile.touch(monitor);
						iClasspathFile.refreshLocal(IResource.DEPTH_ZERO, monitor);
					} catch (CoreException e) {
						FeatureHouseCorePlugin.getDefault().logError(e);
					}
				}
				return Status.OK_STATUS;
			}
		};
		job.setPriority(Job.DECORATE);
		job.schedule();
		
	}

	/**
	 * Sets the Java build path to the folder at the build folder, named like the current configuration.
	 * @param buildPath The name of the current configuration
	 */
	private void setJavaBuildPath(String buildPath) {
		try {
			JavaProject javaProject = new JavaProject(featureProject.getProject(), null);
			IClasspathEntry[] classpathEntrys = javaProject.getRawClasspath();
			
			int i = 0;
			for (IClasspathEntry e : classpathEntrys) {
				if (e.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					IPath path = featureProject.getBuildFolder().getFolder(buildPath).getFullPath();
					
					/** return if nothing has to be changed **/
					if (e.getPath().equals(path)) {
						return;
					}
					
					/** change the actual source entry to the new build path **/
					ClasspathEntry changedEntry = new ClasspathEntry(e.getContentKind(), e.getEntryKind(), 
							path, e.getInclusionPatterns(), e.getExclusionPatterns(), 
							e.getSourceAttachmentPath(), e.getSourceAttachmentRootPath(), null, 
							e.isExported(), e.getAccessRules(), e.combineAccessRules(), e.getExtraAttributes());
					classpathEntrys[i] = changedEntry;
					javaProject.setRawClasspath(classpathEntrys, null);
					return;
				}
				i++;
			}
			
			/** case: there is no source entry at the class path
			 *  	  add the source entry to the classpath **/
			IFolder folder = featureProject.getBuildFolder().getFolder(buildPath);
			ClasspathEntry sourceEntry = new ClasspathEntry(IPackageFragmentRoot.K_SOURCE, 
					IClasspathEntry.CPE_SOURCE, folder.getFullPath(), new IPath[0], new IPath[0], 
					null, null, null, false, null, false, new IClasspathAttribute[0]);
			IClasspathEntry[] newEntrys = new IClasspathEntry[classpathEntrys.length + 1];
			System.arraycopy(classpathEntrys, 0, newEntrys, 0, classpathEntrys.length);
			newEntrys[newEntrys.length - 1] = sourceEntry;
			javaProject.setRawClasspath(newEntrys, null);
		} catch (JavaModelException e) {
			FeatureHouseCorePlugin.getDefault().logError(e);
		}
	}
	
	/**
	 * For <code>FeatureHouse<code> a clean does not remove the folder,
	 * named like the current configuration at the build folder, 
	 * to prevent build path errors.
	 * @return always <code>false</code>
	 */
	@Override
	public boolean clean() {
		if (featureProject == null || featureProject.getBuildFolder() == null) {
			return false;
		}
		IFile config = featureProject.getCurrentConfiguration();
		if (config == null) {
			return false;
		}
		try {
			for (IResource featureFolder : featureProject.getBuildFolder().members()) {
				if (featureFolder.getName().equals(config.getName().split("[.]")[0])) {
					if (featureFolder instanceof IFolder) {
						for (IResource res : ((IFolder)featureFolder).members()){
							res.delete(true, null);
						}
					} else {
						featureFolder.delete(true, null);
					}
				} else {
					featureFolder.delete(true, null);
				}
			}
		} catch (CoreException e) {
			FeatureHouseCorePlugin.getDefault().logError(e);
		}
		return false;
	}
	
	public static final LinkedHashSet<String> EXTENSIONS = createExtensions(); 
	
	private static LinkedHashSet<String> createExtensions() {
		LinkedHashSet<String> extensions = new LinkedHashSet<String>();
		extensions.add("java");
		extensions.add("cs");
		extensions.add("c");
		extensions.add("h");
		extensions.add("hs");
		extensions.add("jj");
		extensions.add("als");
		extensions.add("xmi");
		return extensions;
	}  

	@Override
	public LinkedHashSet<String> extensions() {
		return EXTENSIONS;
	}
	



	@Override
	public ArrayList<String[]> getTemplates() {
		return TEMPLATES;
	}
	
	private static final ArrayList<String[]> TEMPLATES = createTempltes();
	
	private static ArrayList<String[]> createTempltes() {
		 ArrayList<String[]> list = new  ArrayList<String[]>(8);
		 list.add(new String[]{"Alloy", "als", "module " + CLASS_NAME_PATTERN});
		 list.add(new String[]{"C", "c", ""});
		 list.add(new String[]{"C#", "cs", "public class " + CLASS_NAME_PATTERN + " {\n\n}"});
		 list.add(new String[]{"Haskell", "hs", "module " + CLASS_NAME_PATTERN + " where \n{\n\n}"});
		 list.add(JAVA_TEMPLATE);
		 list.add(new String[]{"JavaCC", "jj", "PARSER_BEGIN(" + CLASS_NAME_PATTERN + ") \n \n PARSER_END(" + CLASS_NAME_PATTERN + ")"});
		 list.add(new String[]{"UML", "xmi", "<?xml version = '1.0' encoding = 'UTF-8' ?> \n	<XMI xmi.version = '1.2' xmlns:UML = 'org.omg.xmi.namespace.UML'>\n\n</XMI>"});
		 list.add(new String[]{ "Jak", "jak", "/**\r\n * TODO description\r\n */\r\npublic " + REFINES_PATTERN + " class " + CLASS_NAME_PATTERN + " {\r\n\r\n}" });
		 return list;
	}

	@Override
	public void postCompile(IResourceDelta delta, final IFile file) {
		super.postCompile(delta, file);
		try {
			if (!file.getWorkspace().isTreeLocked()) {
				file.refreshLocal(IResource.DEPTH_ZERO, null);
			}
			errorPropagation.addFile(file);
		} catch (CoreException e) {
			FeatureHouseCorePlugin.getDefault().logError(e);
		}
	}

	@Override
	public int getDefaultTemplateIndex() {
		return 4;
	}

	@Override
	public void buildFSTModel() {
		if (featureProject == null) {
			return;
		}
		final String configPath;
		IFile currentConfiguration = featureProject.getCurrentConfiguration();
		if (currentConfiguration != null) {
			configPath = currentConfiguration.getRawLocation().toOSString();
		} else {
			configPath = featureProject.getProject().getFile(".project").getRawLocation().toOSString();
		}
		final String basePath = featureProject.getSourcePath();
		final String outputPath = featureProject.getBuildPath();
		if (configPath == null || basePath == null || outputPath == null)
			return;
		
		composer = new FSTGenComposerExtension();
		composer.addParseErrorListener(listener);

		List<String> featureOrderList = featureProject.getFeatureModel().getConcreteFeatureNames();
		String[] features = new String[featureOrderList.size()];
		int i = 0;
		for (String f : featureOrderList) {
			features[i++] = f;
		}
		
		try {
			((FSTGenComposerExtension)composer).buildFullFST(new String[] {
					CmdLineInterpreter.INPUT_OPTION_EQUATIONFILE, configPath,
					CmdLineInterpreter.INPUT_OPTION_BASE_DIRECTORY, basePath,
					CmdLineInterpreter.INPUT_OPTION_OUTPUT_DIRECTORY, outputPath + "/",
					CmdLineInterpreter.INPUT_OPTION_CONTRACT_STYLE, getContractParameter()
			}, features);
		} catch (TokenMgrError e) {
			createBuilderProblemMarker(getTokenMgrErrorLine(e.getMessage()), getTokenMgrErrorMessage(e.getMessage()));
		} catch (Error e) {
			FeatureHouseCorePlugin.getDefault().logError(e);
		}
		
		ArrayList<FSTNode> fstnodes = composer.getFstnodes();
		if (fstnodes != null) {
			fhModelBuilder.buildModel(fstnodes, false);
			fstnodes.clear();
		}
	}

	@Override
	public void buildConfiguration(IFolder folder, Configuration configuration, String congurationName) {
		String folderName = folder.getName();
		super.buildConfiguration(folder, configuration, folderName);
		IFile configurationFile = folder.getFile(folderName + '.' + getConfigurationExtension());
		FSTGenComposer composer = new FSTGenComposer(false);
		composer.addParseErrorListener(createParseErrorListener());
		composer.run(new String[]{
				CmdLineInterpreter.INPUT_OPTION_EQUATIONFILE, configurationFile.getRawLocation().toOSString(),
				CmdLineInterpreter.INPUT_OPTION_BASE_DIRECTORY, featureProject.getSourcePath(),
				CmdLineInterpreter.INPUT_OPTION_OUTPUT_DIRECTORY, folder.getParent().getLocation().toOSString() + "/",
				CmdLineInterpreter.INPUT_OPTION_CONTRACT_STYLE, getContractParameter()
		}); 
		if (errorPropagation.job != null) {
			/*
			 * Waiting for the propagation job to finish, 
			 * because the corresponding FSTModel is necessary for propagation at FH
			 * This is in general no problem because the compiler is much faster then the composer
			 */
			try {
				errorPropagation.job.join();
			} catch (InterruptedException e) {
				FeatureHouseCorePlugin.getDefault().logError(e);
			}
		}
		fhModelBuilder.buildModel(composer.getFstnodes(), false);
		if (!configurationFile.getName().startsWith(congurationName)) {
			try {
				configurationFile.move(((IFolder)configurationFile.getParent()).getFile(congurationName + '.' + getConfigurationExtension()).getFullPath(), true, null);
			} catch (CoreException e) {
				FeatureHouseCorePlugin.getDefault().logError(e);
			}
		}
	}
	
	/**
	 * FeatureHouse causes access violation errors if it is executed parallel.
	 */
	@Override
	public boolean canGeneratInParallelJobs() {
		return false;
	}
	
	public static final QualifiedName BUILD_META_PRODUCT = 
			new QualifiedName(FeatureHouseComposer.class.getName() +"#BuildMetaProduct", 
							  FeatureHouseComposer.class.getName() +"#BuildMetaProduct");
	public static final QualifiedName VERIFY_PRODUCT = 
			new QualifiedName(FeatureHouseComposer.class.getName() +"#VerifyProduct", 
							  FeatureHouseComposer.class.getName() +"#VerifyProduct");
	
	private static final String TRUE = "true";
	
	public final boolean buildMetaProduct() {
		try {
			return TRUE.equals(featureProject.getProject().getPersistentProperty(BUILD_META_PRODUCT));
		} catch (CoreException e) {
			FMCorePlugin.getDefault().logError(e);
		}
		return false;
	}
	
	public final boolean verifyProduct() {
		try {
			return TRUE.equals(featureProject.getProject().getPersistentProperty(VERIFY_PRODUCT));
		} catch (CoreException e) {
			FMCorePlugin.getDefault().logError(e);
		}
		return false;
	}

	@Override
	public boolean hasContractComposition() {
		return true;
	}
	
	@Override
	public boolean hasMetaProductGeneration() {
		return true;
	}
	
	@Override
	public void copyNotComposedFiles(Configuration config, IFolder destination) {
		if (destination.getProject().equals(featureProject.getProject())) {
			super.copyNotComposedFiles(config, destination.getFolder(
					featureProject.getCurrentConfiguration().getName().split("[.]")[0]));
		} else {
			// case: build into an external project
			super.copyNotComposedFiles(config, destination);
		}

	}
	
}
