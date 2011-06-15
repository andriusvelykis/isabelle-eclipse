package isabelle.eclipse.launch.config;

import isabelle.eclipse.launch.IsabelleLaunchConstants;
import isabelle.eclipse.launch.IsabelleLaunchPlugin;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;


public class RootDirLaunch extends IsabelleLaunch {

	@Override
	protected String getInstallationPath(ILaunchConfiguration configuration) throws CoreException {
		return getVerifyPath(configuration).getPath();
	}

	private File getVerifyPath(ILaunchConfiguration configuration) throws CoreException {
		
		String location = getLocationConfig(configuration);
		
		if (location == null || location.isEmpty()) {
			abort("Isabelle location not specified");
		}
		
		// allow subclasses to adapt the actual path
		location = adaptPath(location);
		
		File file = new File(location);
		if (!file.exists()) {
			abort("Isabelle location does not exist");
		}
		
		// must be a directory
		if (!file.isDirectory()) {
			abort(getNotDirectoryMessage());
		}
		
		return file;
	}
	
	protected String adaptPath(String path) {
		return path;
	}
	
	protected String getNotDirectoryMessage() {
		return "Isabelle installation location is not a directory";
	}
	
	public static String getLocationConfig(ILaunchConfiguration configuration) {
		String location= ""; //$NON-NLS-1$
		try {
			location= configuration.getAttribute(IsabelleLaunchConstants.ATTR_LOCATION, ""); //$NON-NLS-1$
		} catch (CoreException ce) {
			IsabelleLaunchPlugin.getDefault().log("Error reading configuration", ce);
		}
		return location;
	}
	
}
