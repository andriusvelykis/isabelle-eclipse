package isabelle.eclipse.launch.tabs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class LogDialog extends MessageDialog {

	private final String log;
	
	public LogDialog(Shell parentShell, String dialogTitle, String dialogMessage, String log, int style) {
		
		super(parentShell, dialogTitle, null, dialogMessage, 
				INFORMATION, new String[] { IDialogConstants.OK_LABEL }, 0);
		
		setShellStyle(getShellStyle() | style | SWT.RESIZE);
		
		if (log == null) {
			log = "";
		}
		
		this.log = log;
	}

	@Override
	protected Control createCustomArea(Composite parent) {
		
		Text logField = new Text(parent, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		gridData.heightHint = 200;
		logField.setLayoutData(gridData);
		
		logField.setText(log);

		return logField;
	}

}
