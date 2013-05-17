package isabelle.eclipse.diagnostic;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;


/**
 * Performs diagnostics about the system: checks that a correct JRE is used.
 * 
 * @author Andrius Velykis
 */
public class IsabelleDiagnosticStartup implements IStartup {

  public void earlyStartup() {
    // check and report that appropriate Java version is used
    checkJavaVersion();
  }

  private static void checkJavaVersion() {
    String versionStr = System.getProperty("java.version");

    // try parsing the Java version
    Double version;
    try {
      version = parseVersionNumber(versionStr);
    } catch (Exception ex) {
      IsabelleDiagnosticPlugin.logError(null, ex);
      version = null;
    }

    // require Java 7
    if (version == null || version < 1.7) {
      reportInvalidJava(versionStr);
    }
  }

  private static double parseVersionNumber(String versionStr) {
    int pos = 0;
    int count = 0;
    for (; pos < versionStr.length() && count < 2; pos++) {
      if (versionStr.charAt(pos) == '.') {
        count++;
      }
    }

    return Double.parseDouble(versionStr.substring(0, pos - 1));
  }

  private static void reportInvalidJava(final String currentVersion) {

    final IWorkbench workbench = PlatformUI.getWorkbench();
    workbench.getDisplay().asyncExec(new Runnable() {
      public void run() {
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

        String msg = "Bad Java version - Isabelle/Eclipse plugins will not be available.\n\n"
            + "Isabelle/Eclipse requires Java 7 to run (version >= 1.7).\n"
            + "Found Java version: " + currentVersion + ".\n\n"
            + "Ensure that Java 7 (e.g. JDK - Java Development Kit) is installed in your system. "
            + "If Java 7 is not selected automatically, indicate it explictly in eclipse.ini:";

        String link = "http://wiki.eclipse.org/Eclipse.ini";

        if (window != null) {
          MessageDialog dialog = new ErrorDialogWithLink(
              window.getShell(), "Invalid Java version", msg, link);

          dialog.open();
        } else {
          // at least report to the error log
          IsabelleDiagnosticPlugin.logError(msg + " " + link, null);
        }
      }
    });

  }

  private static class ErrorDialogWithLink extends MessageDialog {

    private final String hyperlink;

    public ErrorDialogWithLink(Shell parentShell, String dialogTitle, String dialogMessage,
        String hyperlink) {
      super(parentShell, dialogTitle, null, dialogMessage, MessageDialog.ERROR,
          new String[] { IDialogConstants.OK_LABEL }, 0);

      setShellStyle(getShellStyle() | SWT.SHEET);

      this.hyperlink = hyperlink;
    }

    @Override
    protected Control createMessageArea(Composite composite) {

      Control parent = super.createMessageArea(composite);

      Composite dummy = new Composite(composite, SWT.NONE);
      dummy.setLayoutData(GridDataFactory.fillDefaults().hint(0, 0).create());

      // place the link in the custom area
      Link link = new Link(composite, SWT.NONE);
      String linkText = "<a href=\"" + hyperlink + "\">" + hyperlink + "</a>";
      link.setText(linkText);

      link.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          openLinkInBrowser(e.text);
        }
      });

      return parent;
    }

    private void openLinkInBrowser(String link) {
      try {
        // Open default external browser
        IWebBrowser br = PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser();
        br.openURL(new URL(link));
      } catch (PartInitException ex) {
        IsabelleDiagnosticPlugin.logError(null, ex);
      } catch (MalformedURLException ex) {
        IsabelleDiagnosticPlugin.logError(null, ex);
      }
    }
  }

}
