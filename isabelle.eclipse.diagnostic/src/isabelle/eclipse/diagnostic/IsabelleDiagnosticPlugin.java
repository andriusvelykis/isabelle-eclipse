package isabelle.eclipse.diagnostic;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


/**
 * The activator class controls the plug-in life cycle
 * 
 * @author Andrius Velykis
 */
public class IsabelleDiagnosticPlugin extends AbstractUIPlugin {

  // The plug-in ID
  public static final String PLUGIN_ID = "isabelle.eclipse.diagnostic"; //$NON-NLS-1$

  // The shared instance
  private static IsabelleDiagnosticPlugin plugin;

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
   */
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
   */
  public void stop(BundleContext context) throws Exception {
    plugin = null;
    super.stop(context);
  }

  /**
   * Returns the shared instance
   * 
   * @return the shared instance
   */
  public static IsabelleDiagnosticPlugin getDefault() {
    return plugin;
  }
  
  public static void logError(String message, Throwable exception) {
    // if message is not given, try to use exception's
    String msg = message != null ? message : exception.getMessage();
    IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, 0, msg, exception);

    getDefault().getLog().log(status);
  }

}
