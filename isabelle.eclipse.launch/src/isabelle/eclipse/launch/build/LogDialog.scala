package isabelle.eclipse.launch.build

import org.eclipse.jface.dialogs.{IDialogConstants, MessageDialog}
import org.eclipse.jface.layout.GridDataFactory
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.{Composite, Control, Shell, Text}


/**
 * A dialog to display log message in a large text area.
 *
 * @author Andrius Velykis
 */
class LogDialog(parentShell: Shell,
                dialogTitle: String,
                dialogMessage: String,
                log: String = "")
    extends MessageDialog(parentShell,
                          dialogTitle,
                          null,
                          dialogMessage,
                          MessageDialog.INFORMATION,
                          Array[String](IDialogConstants.OK_LABEL),
                          0) {

  setShellStyle(SWT.DIALOG_TRIM | SWT.MAX | SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE)


  override def createCustomArea(parent: Composite): Control = {

    val logField = new Text(parent, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL)
    logField.setLayoutData(GridDataFactory.
      fillDefaults().
      grab(true, true).
      hint(IDialogConstants.ENTRY_FIELD_WIDTH, 200).create)

    logField.setText(log)

    logField
  }

}
