package isabelle.eclipse.launch.tabs;

import isabelle.eclipse.launch.config.AppBundleLaunch;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;


public class AppBundleMainTab extends RootDirMainTab {

	protected String getLocationLabel() {
		return "Isabelle application bundle (.app) location:";
	}
	
	@Override
	protected String getDefaultLocationMessage() {
		return "Please specify the location of the Isabelle application bundle (.app file) you would like to configure";
	}
	
	@Override
	protected String getInstallationPath() {
		String enteredPath = super.getInstallationPath();
		return AppBundleLaunch.adaptBundlePath(enteredPath);
	}

	@Override
	protected boolean validateFile(File file, boolean newConfig) {
		// must be a directory, even though we are using FileDialog - .app bundle is actually a directory
		if (!file.isDirectory()) {
			if (!newConfig) {
				setErrorMessage("Isabelle installation location specified is not an application bundle file");
			}
			return false;
		}
		
		return true;
	}

	/**
	 * Prompts the user to choose a location from the filesystem and
	 * sets the location as the full path of the selected file.
	 */
	@Override
	protected void handleFileLocationButtonSelected() {
		FileDialog fileDialog = new FileDialog(getShell(), SWT.NONE);
		fileDialog.setFileName(locationField.getText());
		String text= fileDialog.open();
		if (text != null) {
			locationField.setText(text);
		}
	}
	
}
