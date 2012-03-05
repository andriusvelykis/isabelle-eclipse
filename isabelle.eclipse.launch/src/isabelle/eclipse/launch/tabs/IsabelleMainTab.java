package isabelle.eclipse.launch.tabs;

import java.util.List;

import isabelle.eclipse.launch.IsabelleLaunchConstants;
import isabelle.eclipse.launch.IsabelleLaunchImages;
import isabelle.eclipse.launch.config.IsabelleLaunch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;


public abstract class IsabelleMainTab extends AbstractLaunchConfigurationTab {

	public final static String FIRST_EDIT = "editedByIsaInstallationTab"; //$NON-NLS-1$

	private CheckboxTableViewer logicsViewer;
	private Button logicsLogButton;
	
	private boolean fInitializing= false;
	private boolean userEdited= false;

	private WidgetListener fListener= new WidgetListener();
	
	/**
	 * A listener to update for text modification and widget selection.
	 */
	private class WidgetListener extends SelectionAdapter implements ICheckStateListener {
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			setDirty(true);
			Object source= e.getSource();
			if (source == logicsLogButton) {
				handleLogicsLogButtonSelected();
			}
		}
		
		@Override
		public void checkStateChanged(CheckStateChangedEvent event) {
			configModified();
		}

	}
	
	protected void configModified() {
		if (!fInitializing) {
			setDirty(true);
			userEdited= true;
			updateLaunchConfigurationDialog();
		}
	}
	
	protected abstract String getInstallationPath();
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite mainComposite = new Composite(parent, SWT.NONE);
		setControl(mainComposite);
		mainComposite.setFont(parent.getFont());
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		mainComposite.setLayout(layout);
		mainComposite.setLayoutData(gridData);

		createInstallationConfigComponent(mainComposite);
		createLogicsComponent(mainComposite);
		createVerticalSpacer(mainComposite, 1);
		
		Dialog.applyDialogFont(parent);
	}
	
	protected void createInstallationConfigComponent(Composite parent) {
		// do nothing by default
	}
	
	/**
	 * Creates the controls needed to select logic for the Isabelle installation
	 *
	 * @param parent the composite to create the controls in
	 */
	private void createLogicsComponent(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		String groupName = "&Logic:";
		group.setText(groupName); 
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		GridData gridData = new GridData(GridData.FILL_BOTH);
		group.setLayout(layout);
		group.setLayoutData(gridData);
        group.setFont(parent.getFont());
        
		logicsViewer = CheckboxTableViewer.newCheckList(group, SWT.CHECK | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		gridData.heightHint = 50;
		logicsViewer.getControl().setLayoutData(gridData);
		logicsViewer.setLabelProvider(new LogicLabelProvider());
		logicsViewer.setContentProvider(new ArrayContentProvider());
		
		logicsViewer.addCheckStateListener(new SingleCheckedListener(logicsViewer));
		
		logicsViewer.setInput(new String[0]);

		logicsViewer.addCheckStateListener(fListener);
		
		addControlAccessibleListener(logicsViewer.getControl(), group.getText());
		
		Composite composite = new Composite(group, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns= 1;
        layout.marginHeight= 0;
        layout.marginWidth= 0;
		gridData = new GridData(SWT.END, SWT.BEGINNING, true, false);
		composite.setLayout(layout);
		composite.setLayoutData(gridData);
		composite.setFont(parent.getFont());
		
		logicsLogButton= createPushButton(composite, "Show Log...", null);
		logicsLogButton.addSelectionListener(fListener);
		logicsLogButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		addControlAccessibleListener(logicsLogButton, logicsLogButton.getText()); // need to strip the mnemonic from buttons
		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(FIRST_EDIT, true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		fInitializing= true;
		updateInstallationConfig(configuration);
		updateLogic(configuration);
		fInitializing= false;
		setDirty(false);
	}
	
	protected void updateInstallationConfig(ILaunchConfiguration configuration) {
		// do nothing by default
	}
	
	/**
	 * Updates the drivers widgets to match the state of the given launch
	 * configuration.
	 */
	private void updateLogic(ILaunchConfiguration configuration) {
		
		String logic = IsabelleLaunch.getLogicConfig(configuration);
		reloadLogics();
		setSelectedLogic(logic);
	}

	private String getSelectedLogic() {
		Object[] selectedLogics = logicsViewer.getCheckedElements(); 
		
		if (selectedLogics.length > 0) {
			return (String) selectedLogics[0];
		}
		
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		performApplyInstallationConfig(configuration);
		
		String logic = getSelectedLogic();
		configuration.setAttribute(IsabelleLaunchConstants.ATTR_LOGIC, logic);
		
		if(userEdited) {
			configuration.setAttribute(FIRST_EDIT, (String)null);
		}
	}
	
	protected void performApplyInstallationConfig(ILaunchConfigurationWorkingCopy configuration) {
		// do nothing by default
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	@Override
	public String getName() {
		return "Main";
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		setErrorMessage(null);
		setMessage(null);
		boolean newConfig = false;
		try {
			newConfig = launchConfig.getAttribute(FIRST_EDIT, false);
		} catch (CoreException e) {
			//assume false is correct
		}
		
		if (!validateInstallationConfig(newConfig)) {
			return false;
		}
		
		return validateLogic(newConfig);
	}
	
	protected boolean validateInstallationConfig(boolean newConfig) {
		return true;
	}
	
	/**
	 * Validates the logic selection.
	 */
	private boolean validateLogic(boolean newConfig) {
		
		// nothing in the logic table
		if (logicsViewer.getTable().getItemCount() == 0) {
			setErrorMessage("There are no Isabelle logics available in the indicated location");
			setMessage(null);
			return false;
		}
		
		String driver = getSelectedLogic();
		if (driver == null) {
			
			if (newConfig) {
				setErrorMessage(null);
				setMessage("Please select an Isabelle logic for the indicated location");
			} else {
				setErrorMessage("Isabelle logic must be selected");
				setMessage(null);
			}
			
			return false;
		}
		
		return true;
	}
	
	protected void installationConfigChanged() {
		reloadLogics();
	}
	
    private void reloadLogics() {
    	
    	String installationPath = getInstallationPath();
    	List<String> logics = IsabelleLaunch.loadLogics(installationPath);

		logicsViewer.setInput(logics);
        
        // set explicitly if nothing was set before
        if (logics.size() == 1) {
        	setSelectedLogic(logics.get(0));
        }
        
    }
    
	private void setSelectedLogic(String driver) {
		String[] selectLogics = driver != null ? new String[] {driver} : new String[0];
		logicsViewer.setCheckedElements(selectLogics);
	}
	
	private void handleLogicsLogButtonSelected() {
		
		LogDialog logDialog = new LogDialog(getShell(), "Logics Query Log", 
				// FIXME
				"Querying Isabelle logics available in the indicated location.", "TODO", SWT.NONE);
		logDialog.open();		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	@Override
	public Image getImage() {
		return IsabelleLaunchImages.getImage(IsabelleLaunchImages.IMG_TAB_MAIN);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#deactivated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	@Override
	public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#activated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	@Override
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
	}
	
	/*
	 * Fix for Bug 60163 Accessibility: New Builder Dialog missing object info for textInput controls
	 */
	public void addControlAccessibleListener(Control control, String controlName) {
		//strip mnemonic (&)
		String[] strs = controlName.split("&"); //$NON-NLS-1$
		StringBuffer stripped = new StringBuffer();
		for (int i = 0; i < strs.length; i++) {
			stripped.append(strs[i]);
		}
		control.getAccessible().addAccessibleListener(new ControlAccessibleListener(stripped.toString()));
	}
	
	private class ControlAccessibleListener extends AccessibleAdapter {
		private String controlName;
		ControlAccessibleListener(String name) {
			controlName = name;
		}
		@Override
		public void getName(AccessibleEvent e) {
			e.result = controlName;
		}
		
	}
	
}
