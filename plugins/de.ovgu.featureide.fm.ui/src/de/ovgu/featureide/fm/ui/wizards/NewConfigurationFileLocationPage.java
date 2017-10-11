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

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

import de.ovgu.featureide.fm.ui.FMUIPlugin;

/**
 * 
 * @author Sebastian Krieter
 * @author Marlen Bernier
 */
public class NewConfigurationFileLocationPage extends WizardNewFileCreationPage {

	String warningMessage = "";

	public NewConfigurationFileLocationPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
		setTitle("Choose Location");
		setDescription("Select a path to the new Configuration file.");
		setMessage(warningMessage);

	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
		if (!FMUIPlugin.getDefault().isOnlyFeatureModelingInstalled()) {

			/**
			 * The following should only happen when the user chooses a FeatureIDE project and if not only feature modeling is installed
			 */
			if (this.getContainerFullPath() != null && this.getContainerFullPath().segmentCount() >= 1) {

				IResource res2 = ResourcesPlugin.getWorkspace().getRoot().getProject(this.getContainerFullPath().segment(0).toString());

				if (FMUIPlugin.getDefault().setProjectResource(res2)) {
					if (!this.getContainerFullPath().lastSegment().toString().equals("configs")) {
						warningMessage = "For FeatureIDE Projects it is recommended to use a 'configs' folder for configurations";
						setMessage(warningMessage, 1);
					} else {
						warningMessage = "";
						setMessage(warningMessage, 0);
					}

				} else {
					warningMessage = "";
					setMessage(warningMessage, 0);
				}
			}

		}
	}
}
