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

import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.fm.ui.extensionpoint.TodoExtensionInterface;

/**
 * TODO description
 * 
 * @author Marlen Bernier
 * @author Dawid Szczepanski
 */
public class TodoExtensionChecker implements TodoExtensionInterface {

	/* 
	 * @see de.ovgu.featureide.fm.ui.extensionpoint.TodoExtensionInterface#extensionMethod()
	 */
	@Override
	public boolean extensionMethod(IResource res) {
		System.out.println("guten tach");
		IFeatureProject project = CorePlugin.getFeatureProject(res);
		if(project == null){
			return false;
		}else {
			if(project.getConfigFolder().toString().contains("configs")){
				System.out.println("configs ist gleich configs");
			}
			System.out.println(project.getConfigFolder());
			return true;
		}
		
	}
}
