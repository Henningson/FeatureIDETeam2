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
package de.ovgu.featureide.fm.core.io.xml;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import org.prop4j.Node;
import org.prop4j.Or;

import de.ovgu.featureide.common.Commons;
import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.IFeatureModelFactory;
import de.ovgu.featureide.fm.core.base.impl.FMFactoryManager;
import de.ovgu.featureide.fm.core.io.IFeatureModelFormat;
import de.ovgu.featureide.fm.core.io.TAbstractFeatureModelReaderWriter;
import de.ovgu.featureide.fm.core.io.UnsupportedModelException;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import de.ovgu.featureide.fm.ui.editors.IGraphicalFeature;
import de.ovgu.featureide.fm.ui.editors.IGraphicalFeatureModel;
import de.ovgu.featureide.fm.ui.editors.elements.GraphicalFeatureModel;

/**
 * test class for XmlReader/Writer
 *
 * @author Fabian Benduhn
 * @author Dawid Szczepanski
 * @author Marlen Bernier
 * @author Christopher Sontag
 * @author Maximilian Kühl
 */
public class TXmlFeatureModelFormat extends TAbstractFeatureModelReaderWriter {

	/**
	 * @param file
	 * @throws UnsupportedModelException
	 */
	public TXmlFeatureModelFormat(IFeatureModel fm, String s) throws UnsupportedModelException {
		super(fm, s);
		
	}
	
	@Test
	public void testFeatureCollapsed() throws FileNotFoundException, UnsupportedModelException {

		final IFeatureModel fmOrig =
			Commons.loadFeatureModelFromFile("basic.xml", Commons.FEATURE_MODEL_TESTFEATUREMODELS_PATH_REMOTE,
					Commons.FEATURE_MODEL_TESTFEATUREMODELS_PATH_LOCAL_CLASS_PATH);
		final IFeatureModel fmCollapsed =
			Commons.loadFeatureModelFromFile("basic_collapsed.xml", Commons.FEATURE_MODEL_TESTFEATUREMODELS_PATH_REMOTE,
					Commons.FEATURE_MODEL_TESTFEATUREMODELS_PATH_LOCAL_CLASS_PATH);
		final IFeatureModel fmNotCollapsed =
			Commons.loadFeatureModelFromFile("basic_not_collapsed.xml", Commons.FEATURE_MODEL_TESTFEATUREMODELS_PATH_REMOTE,
					Commons.FEATURE_MODEL_TESTFEATUREMODELS_PATH_LOCAL_CLASS_PATH);

		final IGraphicalFeatureModel gFM =
			new GraphicalFeatureModel(fmOrig);
		gFM.init();

		final IGraphicalFeatureModel gfmCollapsed =
			new GraphicalFeatureModel(fmCollapsed);
		gfmCollapsed.init();
		for (final IGraphicalFeature feature : gfmCollapsed.getFeatures()) {
			if (feature.getObject().getName().equals("Root")) {
				feature.setCollapsed(true);
			}
		}

		final IGraphicalFeatureModel gfmNotCollapsed =
			new GraphicalFeatureModel(fmNotCollapsed);
		gfmNotCollapsed.init();
		gfmCollapsed.init();
		for (final IGraphicalFeature feature : gfmCollapsed.getFeatures()) {
			feature.setCollapsed(false);
		}

		assertEquals(gFM.getVisibleFeatures().size(), gfmCollapsed.getFeatures().size());

		int notVisible =
			0;
		for (final IGraphicalFeature feature : gfmCollapsed.getFeatures()) {
			if (feature.hasCollapsedParent()) {
				notVisible++;
			}
		}

		assertEquals(gFM.getVisibleFeatures().size(), gfmCollapsed.getVisibleFeatures().size()
			+ notVisible);

		assertEquals(gFM.getVisibleFeatures().size(), gfmNotCollapsed.getVisibleFeatures().size());
	}

	@Test
	public void testConstraintDescription() throws FileNotFoundException, UnsupportedModelException {
		String constraindescriptionFromXml =
			"";

		final IFeatureModel fm =
			Commons.loadFeatureModelFromFile("constraintDescriptionTest.xml", Commons.FEATURE_MODEL_TESTFEATUREMODELS_PATH_REMOTE,
					Commons.FEATURE_MODEL_TESTFEATUREMODELS_PATH_LOCAL_CLASS_PATH);

		assertEquals(1, fm.getConstraints().size());

		for (IConstraint constraint : fm.getConstraints()) {
			constraindescriptionFromXml =
				constraint.getDescription();
			assertEquals(constraindescriptionFromXml, "Test Description");

		}
	}

	@Test
	public void testConstraintDescriptionTwoRules() throws FileNotFoundException, UnsupportedModelException {
		String constraindescriptionFromXml =
			"";

		final IFeatureModel fm =
			Commons.loadFeatureModelFromFile("constraintDescriptionTwoRulesTest.xml", Commons.FEATURE_MODEL_TESTFEATUREMODELS_PATH_REMOTE,
					Commons.FEATURE_MODEL_TESTFEATUREMODELS_PATH_LOCAL_CLASS_PATH);

		assertEquals(2, fm.getConstraints().size());

		for (IConstraint constraint : fm.getConstraints()) {
			constraindescriptionFromXml =
				constraint.getDescription();
			assertEquals(constraindescriptionFromXml, "Test Description");

		}
	}

	private IFeatureModel prepareFeatureModel() {
		final IFeatureModelFactory factory =
			FMFactoryManager.getDefaultFactory();

		// setup a test model
		newFm =
			factory.createFeatureModel();
		final IFeature root =
			factory.createFeature(newFm, "root");

		newFm.addFeature(root);
		newFm.getStructure().setRoot(root.getStructure());

		final IFeature A =
			factory.createFeature(newFm, "A");
		final IFeature B =
			factory.createFeature(newFm, "B");
		final IFeature C =
			factory.createFeature(newFm, "C");
		// IFeature D = factory.createFeature(newFm, "D");

		A.getStructure().setMandatory(false);
		B.getStructure().setMandatory(false);
		C.getStructure().setMandatory(false);

		A.getStructure().setAbstract(false);
		B.getStructure().setAbstract(false);
		C.getStructure().setAbstract(false);
		// D.getStructure().setMandatory(false);

		newFm.getStructure().getRoot().addChild(A.getStructure());
		newFm.getStructure().getRoot().addChild(B.getStructure());
		newFm.getStructure().getRoot().addChild(C.getStructure());
		// newFm.getStructure().getRoot().addChild(D.getStructure());
		newFm.getStructure().getRoot().setAnd();

		final Node n1 =
			new Or(A, B);
		final Node n2 =
			new Or(B, C);
		// Node n3 = new Implies(new And(new Or(A,B), D), new Not(C));

		final IConstraint c1 =
			factory.createConstraint(newFm, n1);
		c1.setDescription("Test Write Description 1");

		final IConstraint c2 =
			factory.createConstraint(newFm, n2);
		c2.setDescription("Test Write Description 2");
		// IConstraint c3 = factory.createConstraint(newFm, n3);
		newFm.addConstraint(c1);
		newFm.addConstraint(c2);
		// newFm.addConstraint(c3);

		return newFm;
	}

	
	@Test
	public void testConstraintDescriptionWrite() throws FileNotFoundException, UnsupportedModelException {
		IFeatureModel newFm =
			this.prepareFeatureModel();

		String constraindescriptionFromXml =
			"";
		int i =
			1;
		for (IConstraint constraint : newFm.getConstraints()) {
			constraindescriptionFromXml =
				constraint.getDescription();
			assertEquals(constraindescriptionFromXml, "Test Write Description "
				+ i);
			i++;

		}
	}


	@Test
	public final void writeAndReadModel() throws UnsupportedModelException {
		String featureModelFile =
			"constraintDescriptionWriteReadTest.xml";
		Path fmPath =
			Paths.get("bin/"
				+ Commons.FEATURE_MODEL_TESTFEATUREMODELS_PATH_LOCAL_CLASS_PATH
				+ "/"
				+ featureModelFile);

		IFeatureModel newFm =
			this.prepareFeatureModel();
		boolean	result = FeatureModelManager.save(newFm, fmPath);
		assertEquals(true, result);

		
		final IFeatureModel loadedFm =
			Commons.loadFeatureModelFromFile(featureModelFile, Commons.FEATURE_MODEL_TESTFEATUREMODELS_PATH_REMOTE,
					Commons.FEATURE_MODEL_TESTFEATUREMODELS_PATH_LOCAL_CLASS_PATH);

		assertEquals(2, loadedFm.getConstraints().size());
		String constraindescriptionFromXml =
			"";
		int i =
			1;
		for (IConstraint constraint : loadedFm.getConstraints()) {
			constraindescriptionFromXml =
				constraint.getDescription();
			assertEquals(constraindescriptionFromXml, "Test Write Description "
				+ i);
			i++;

		}

	}

	/*
	 * @see de.ovgu.featureide.fm.core.io.TAbstractFeatureModelReaderWriter#getFormat()
	 */
	@Override
	protected IFeatureModelFormat getFormat() {
		return new XmlFeatureModelFormat();
	}
	
}
