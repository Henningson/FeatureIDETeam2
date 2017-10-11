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
package de.ovgu.featureide.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.fm.ui.extensionpoint.ConfigurationWizardWarningMessageExtensionInterface;

/**
 * Returns a warning message, under the following conditions:
 * - The Configuration Wizard is used
 * - Is FeatureIDE Project
 * - The selected folder is not a 'configs' folder
 * - Feature Modeling is not only installed
 * 
 * @author Marlen Bernier
 * @author Dawid Szczepanski
 */
public class ConfigurationWizardWarningMessageExtension implements ConfigurationWizardWarningMessageExtensionInterface {

	protected String warningMessage = "";

	/*
	 * @see de.ovgu.featureide.fm.ui.extensionpoint.ConfigurationWizardWarningMessageExtensionInterface#extensionMethod()
	 */
	@Override
	public boolean extensionMethod(IResource res, IPath chosenPath) {

		IFeatureProject project = CorePlugin.getFeatureProject(res);
		if (project == null) {

			warningMessage = "";
			return false;
		} else {
			String configFolderName = project.getConfigFolder().getName();
			System.out.println(configFolderName);

			if (chosenPath.toString().contains(configFolderName)) {
				warningMessage = "";
			} else {
				warningMessage = "For FeatureIDE Projects it is recommended to use a 'configs' folder for configurations";
			}
			System.out.println(project.getConfigFolder());
			return true;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see de.ovgu.featureide.fm.ui.extensionpoint.ConfigurationWizardWarningMessageExtensionInterface#getMessage()
	 */
	@Override
	public String getMessage() {
		return warningMessage;
	}

}
