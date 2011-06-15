package isabelle.eclipse.launch.config;

import java.io.File;


public class AppBundleLaunch extends RootDirLaunch {

	@Override
	protected String getNotDirectoryMessage() {
		// must be a directory, even though we are using FileDialog - .app bundle is actually a directory
		return "Isabelle installation location is not an application bundle file";
	}

	@Override
	protected String adaptPath(String path) {
		return adaptBundlePath(path);
	}

	public static String adaptBundlePath(String enteredPath) {
		String bundlePath = enteredPath + "/Contents/Resources/Isabelle/";
		File bundleFile = new File(bundlePath);
		
		if (!bundleFile.isDirectory()) {
			// invalid - return the original
			return enteredPath;
		}
		
		return bundlePath;
	}

}
