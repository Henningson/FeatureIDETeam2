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

import static de.ovgu.featureide.fm.core.localization.StringTable.SELECTED_FILE_ALREADY_EXISTS_;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import de.ovgu.featureide.fm.core.ExtensionManager.NoSuchExtensionException;
import de.ovgu.featureide.fm.core.base.impl.ConfigFormatManager;
import de.ovgu.featureide.fm.core.configuration.XMLConfFormat;
import de.ovgu.featureide.fm.core.io.IConfigurationFormat;

/**
 * The NEW wizard page allows setting the container for the new file as well as the file name. The page will only accept file name without the extension OR with
 * the extension that matches the expected one (.config).
 * 
 * @author Christian Becker
 * @author Jens Meinicke
 * @author Marlen Bernier
 * @author Dawid Szczepanski
 */
public class NewConfigurationFileFormatPage extends WizardPage {

	private final List<IConfigurationFormat> formatExtensions = ConfigFormatManager.getInstance().getExtensions();

	private Combo formatCombo;
	
	String description;

	/**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param pageName
	 */
	public NewConfigurationFileFormatPage() {
		super("format");
		setTitle("Choose Format");
		setDescription("Select a format for the new configuration file.");
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		composite.setLayout(layout);

		Label label = new Label(composite, SWT.NULL);
		label.setText("&Format:");
		formatCombo = new Combo(composite, SWT.BORDER | SWT.SINGLE);
		formatCombo.setLayoutData(gd);
		new Label(composite, SWT.NULL);

		initialize();
		addListeners();
		dialogChanged();
		setControl(composite);
	}

	private void addListeners() {
		formatCombo.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {				
				String suffix= getFormat().getSuffix();
				if(!suffix.equals("xml")) {
					description = "Does not support selection and deselection of abstract features!";
				} else {
					description = "Select a format for the new configuration file.";
				}
				dialogChanged();
			}
		});
	}

	private void initialize() {
		for (IConfigurationFormat format : formatExtensions) {
			formatCombo.add(format.getName() + " (*." + format.getSuffix() + ")");
		}
		try {
			formatCombo.select(formatExtensions.indexOf(ConfigFormatManager.getInstance().getExtension(XMLConfFormat.ID)));
		} catch (NoSuchExtensionException e) {
			formatCombo.select(0);
		}
	}

	private void dialogChanged() {
		updateStatus(description);
	}

	private void updateStatus(String message) {
		setMessage(message);
		setPageComplete(message == null);
	}

	public IConfigurationFormat getFormat() {
		return formatExtensions.get(formatCombo.getSelectionIndex());
	}

	@Override
	public boolean isPageComplete() {
		Path path = ((NewConfigurationWizard) getWizard()).getNewFilePath(getFormat());
		final boolean fileExists = Files.exists(path);
		if (fileExists) {
			setErrorMessage(SELECTED_FILE_ALREADY_EXISTS_);
		}
		return !fileExists;
	}
}
