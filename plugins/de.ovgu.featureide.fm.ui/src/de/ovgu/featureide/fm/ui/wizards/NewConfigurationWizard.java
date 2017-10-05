/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2017  FeatureIDE team, University of Magdeburg, Germany
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
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package de.ovgu.featureide.fm.ui.wizards;

import static de.ovgu.featureide.fm.core.localization.StringTable.NEW_CONFIGURATION;
import static de.ovgu.featureide.fm.core.localization.StringTable.NEW_FILE_WAS_NOT_ADDED_TO_FILESYSTEM;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.IFeatureModelFactory;
import de.ovgu.featureide.fm.core.base.impl.FMFactoryManager;
import de.ovgu.featureide.fm.core.configuration.Configuration;
import de.ovgu.featureide.fm.core.io.IConfigurationFormat;
import de.ovgu.featureide.fm.core.io.manager.SimpleFileHandler;
import de.ovgu.featureide.fm.ui.FMUIPlugin;
import de.ovgu.featureide.fm.ui.editors.configuration.ConfigurationEditor;

/**
 * A Wizard to create a new Feature Model file.
 *
 * @author Jens Meinicke
 * @author Marcus Pinnecke
 * @author Marlen Bernier
 * @author Dawid Szczepanski
 */

public class NewConfigurationWizard extends Wizard implements INewWizard {

	public static final String ID = FMUIPlugin.PLUGIN_ID + ".wizard.NewConfigurationWizard";

	private NewConfigurationFileLocationPage locationpage;
	private NewConfigurationFileFormatPage formatPage;
	private String configFolder;

	@Override
	public boolean performFinish() {
		this.initConfigFolder();
		final IConfigurationFormat format = formatPage.getFormat();

		final Path configPath = getNewFilePath(format);
		SimpleFileHandler.save(configPath, new Configuration(defaultFeatureModel()), format);

		assert (Files.exists(configPath)) : NEW_FILE_WAS_NOT_ADDED_TO_FILESYSTEM;

		String fileName = locationpage.getFileName() + "." + format.getSuffix();
		//IFile modelFile = ResourcesPlugin.getWorkspace().getRoot().getFile(locationpage.getContainerFullPath().append(fileName));
		IFile modelFile =  ResourcesPlugin.getWorkspace().getRoot().getFile(locationpage.getContainerFullPath().append(configFolder).append(fileName));
		try {
			// open editor
			FMUIPlugin.getDefault().openEditor(ConfigurationEditor.ID, modelFile);
		} catch (final Exception e) {
			FMUIPlugin.getDefault().logError(e);
		}
		return true;
	}

	public Path getNewFilePath(IConfigurationFormat format) {
		String fileName = locationpage.getFileName();
		
		if (!fileName.matches(".+\\." + Pattern.quote(format.getSuffix()))) {
			fileName += "." + format.getSuffix();
			fileName = configFolder + fileName;
		}
		return getFullPath(fileName);

	}

	/**
	 * Initializes the configuration folder, if it exists use the configuration folder otherwise use none
	 * 
	 * @param configFolderName
	 */
	private void initConfigFolder() {
		if (Files.exists(getFullPath("configs"))) {
			configFolder = "configs/";
		} else {
			configFolder = ""; // configuration folder does not exist, use no sub folder
		}
	}

	private Path getFullPath(String fileName) {
		return Paths.get(ResourcesPlugin.getWorkspace().getRoot().getFile(locationpage.getContainerFullPath().append(fileName)).getLocationURI());
	}

	private IFeatureModel defaultFeatureModel() {
		final IFeatureModelFactory factory = FMFactoryManager.getDefaultFactory();
		IFeatureModel newFm = factory.createFeatureModel();
		final IFeature root = factory.createFeature(newFm, "root");

		newFm.addFeature(root);
		newFm.getStructure().setRoot(root.getStructure());

		return newFm;
	}

	@Override
	public void addPages() {
		setWindowTitle(NEW_CONFIGURATION);
		addPage(locationpage);
		addPage(formatPage);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		formatPage = new NewConfigurationFileFormatPage();
		locationpage = new NewConfigurationFileLocationPage("location", selection);
	}

}
