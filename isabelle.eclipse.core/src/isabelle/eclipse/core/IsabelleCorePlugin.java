package isabelle.eclipse.core;

import isabelle.eclipse.core.app.Isabelle;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

public class IsabelleCorePlugin extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "isabelle.eclipse.core"; //$NON-NLS-1$

	// The shared instance
	private static IsabelleCorePlugin plugin;
	
	private Isabelle isabelle;
	
	/**
	 * The constructor
	 */
	public IsabelleCorePlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		isabelle = new Isabelle();
		
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		
		if (isabelle != null) {
			isabelle.stop();
			isabelle = null;
		}
		
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static IsabelleCorePlugin getDefault() {
		return plugin;
	}
	
	public static Isabelle getIsabelle() {
		return getDefault().isabelle;
	}
	
	/**
	 * Writes the message to the plug-in's log
	 * 
	 * @param message
	 *            the text to write to the log
	 * @param exception
	 *            exception to capture in the log
	 */
	public static void log(String message, Throwable exception) {
		IStatus status = error(message, exception);
		getDefault().getLog().log(status);
	}
	
	/**
	 * Logs the exception to the plug-in's log. Exception's message is used as
	 * the log message.
	 * 
	 * @param exception
	 *            exception to capture in the log
	 */
	public static void log(Throwable exception) {
		log(exception.getMessage(), exception);
	}
	
	/**
	 * Returns a new error {@code IStatus} for this plug-in.
	 * 
	 * @param exception
	 *            exception to wrap in the error {@code IStatus}
	 * @return the error {@code IStatus} wrapping the exception
	 */
	public static IStatus error(Throwable exception) {
		return error(exception.getMessage(), exception);
	}
	
	/**
	 * Returns a new error {@code IStatus} for this plug-in.
	 * 
	 * @param message
	 *            text to have as status message
	 * @param exception
	 *            exception to wrap in the error {@code IStatus}
	 * @return the error {@code IStatus} wrapping the exception
	 */
	public static IStatus error(String message, Throwable exception) {
		if (message == null) {
			message = ""; 
		}
		return new Status(IStatus.ERROR, PLUGIN_ID, 0, message, exception);
	}
	
}
