package isabelle.eclipse.launch;

import static isabelle.eclipse.launch.IsabelleLaunchPlugin.PLUGIN_ID;

public interface IsabelleLaunchConstants {

	/**
	 * String attribute identifying the location of the installation. Default value
	 * is <code>null</code>. Encoding is tool specific.
	 */
	public static final String ATTR_LOCATION = PLUGIN_ID + ".ATTR_LOCATION"; //$NON-NLS-1$
	
	/**
	 * String attribute identifying the location of CygWin. Default value
	 * is <code>C://cygwin</code>. Encoding is tool specific.
	 */
	public static final String ATTR_CYGWIN_LOCATION = PLUGIN_ID + ".ATTR_CYGWIN_LOCATION"; //$NON-NLS-1$

	/**
	 * String attribute identifying the class of selected driver to be
	 * instantiated. Default value is <code>null</code>.
	 */
	public static final String ATTR_DRIVER_CLASS = PLUGIN_ID + ".ATTR_DRIVER_CLASS"; //$NON-NLS-1$

	/**
	 * Boolean attribute indicating whether drivers must be filtered to show
	 * only applicable. Default value is <code>true</code>.
	 */
	public static final String ATTR_DRIVERS_FILTER = PLUGIN_ID + ".DRIVERS_FILTER"; //$NON-NLS-1$

	/**
	 * String attribute identifying the selected logic name. Default value is
	 * <code>null</code>.
	 */
	public static final String ATTR_LOGIC = PLUGIN_ID + ".ATTR_LOGIC"; //$NON-NLS-1$
	
}
