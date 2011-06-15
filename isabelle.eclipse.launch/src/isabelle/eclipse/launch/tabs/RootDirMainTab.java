package isabelle.eclipse.launch.tabs;

import isabelle.eclipse.launch.IsabelleLaunchConstants;
import isabelle.eclipse.launch.config.RootDirLaunch;

import java.io.File;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;


public class RootDirMainTab extends IsabelleMainTab {

	protected Text locationField;
	private Button fileLocationButton;
	private ISWTObservableValue delayedLocationValue;

	private WidgetListener fListener= new WidgetListener();
	
	/**
	 * A listener to update for text modification and widget selection.
	 */
	private class WidgetListener extends SelectionAdapter implements ModifyListener, 
		IValueChangeListener {
		
		@Override
		public void modifyText(ModifyEvent e) {
			configModified();
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			setDirty(true);
			Object source= e.getSource();
			if (source == fileLocationButton) {
				handleFileLocationButtonSelected();
			}
		}
		
		@Override
		public void handleValueChange(ValueChangeEvent event) {
			installationConfigChanged();
			configModified();
		}
		
	}
	

	@Override
	protected String getInstallationPath() {
		
		if (locationField == null || locationField.isDisposed()) {
			return "";
		}
		
		return locationField.getText();
	}

	/**
	 * Creates the controls needed to edit the location
	 * attribute of an external tool
	 * 
	 * @param parent the composite to create the controls in
	 */
	@Override
	protected void createInstallationConfigComponent(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		String locationLabel = getLocationLabel();
		group.setText(locationLabel);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;	
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		group.setLayout(layout);
		group.setLayoutData(gridData);
		
		locationField = new Text(group, SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		locationField.setLayoutData(gridData);
		locationField.addModifyListener(fListener);
		addControlAccessibleListener(locationField, group.getText());
		
		Composite buttonComposite = new Composite(group, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
        layout.marginWidth = 0;   
		layout.numColumns = 1;
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		buttonComposite.setLayout(layout);
		buttonComposite.setLayoutData(gridData);
		buttonComposite.setFont(parent.getFont());
		
		fileLocationButton= createPushButton(buttonComposite, "Browse File System...", null);
		fileLocationButton.addSelectionListener(fListener);
		addControlAccessibleListener(fileLocationButton, group.getText() + " " + fileLocationButton.getText()); //$NON-NLS-1$
		
		delayedLocationValue = SWTObservables.observeDelayedValue(1000,
				SWTObservables.observeText(locationField, SWT.Modify));
		
		delayedLocationValue.addValueChangeListener(fListener);
		
	}
	
	/**
	 * Returns the label used for the location widgets. Subclasses may wish to override.
	 */
	protected String getLocationLabel() {
		return "Isabelle location:";
	}
	
	/**
	 * Updates the location widgets to match the state of the given launch
	 * configuration.
	 */
	@Override
	protected void updateInstallationConfig(ILaunchConfiguration configuration) {
		String location = RootDirLaunch.getLocationConfig(configuration);
		locationField.setText(location);
	}

	@Override
	protected void performApplyInstallationConfig(ILaunchConfigurationWorkingCopy configuration) {
		String location = locationField.getText().trim();
		if (location.length() == 0) {
			location = null;
		}
		configuration.setAttribute(IsabelleLaunchConstants.ATTR_LOCATION, location);
	}
	
	protected String getDefaultLocationMessage() {
		return "Please specify the root directory of the Isabelle installation you would like to configure";
	}

	/**
	 * Validates the content of the location field.
	 */
	@Override
	protected boolean validateInstallationConfig(boolean newConfig) {
		String location = locationField.getText().trim();
		if (location.length() < 1) {
			if (newConfig) {
				setErrorMessage(null);
				setMessage(getDefaultLocationMessage());
			} else {
				setErrorMessage("Isabelle installation location cannot be empty");
				setMessage(null);
			}
			return false;
		}
		
		File file = new File(location);
		if (!file.exists()) { // The file does not exist.
			if (!newConfig) {
				setErrorMessage("Isabelle installation location does not exist");
			}
			return false;
		}
		
		return validateFile(file, newConfig);
	}
	
	protected boolean validateFile(File file, boolean newConfig) {
		// must be a directory
		if (!file.isDirectory()) {
			if (!newConfig) {
				setErrorMessage("Isabelle installation location specified is not a directory");
			}
			return false;
		}
		
		return true;
	}
	
	/**
	 * Prompts the user to choose a location from the filesystem and
	 * sets the location as the full path of the selected directory.
	 */
	protected void handleFileLocationButtonSelected() {
		DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.SAVE);
		dialog.setMessage("Select a Isabelle installation directory:");
		dialog.setFilterPath(locationField.getText());
		String text= dialog.open();
		if (text != null) {
			locationField.setText(text);
		}
	}
	
}
