package isabelle.eclipse.core;

import isabelle.eclipse.core.app.Isabelle;

import org.eclipse.core.runtime.Plugin;
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
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		isabelle = new Isabelle();
		
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
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
	
}
