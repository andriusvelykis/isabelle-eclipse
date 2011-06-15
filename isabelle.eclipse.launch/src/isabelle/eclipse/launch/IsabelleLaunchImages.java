package isabelle.eclipse.launch;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;


public class IsabelleLaunchImages {

	/* Declare Common paths */
	private static URL ICON_BASE_URL= null;

	static {
		String pathSuffix = "icons/"; //$NON-NLS-1$	
		ICON_BASE_URL= IsabelleLaunchPlugin.getDefault().getBundle().getEntry(pathSuffix);
	}
	
	public static final String IMG_TAB_MAIN = ICON_BASE_URL + "main_tab.gif";
	public static final String IMG_TAB_INSTALLATION = ICON_BASE_URL + "isabelle.png";
	public static final String IMG_DRIVER = ICON_BASE_URL + "isabelle.png";
	public static final String IMG_LOGIC = ICON_BASE_URL + "logic_obj.gif";
	
	/**
	 * Returns the <code>Image<code> identified by the given path,
	 * or <code>null</code> if it does not exist.
	 */
	public static Image getImage(String path) {
		ImageRegistry imageRegistry = IsabelleLaunchPlugin.getDefault().getImageRegistry();
		Image image = imageRegistry.get(path);
		if (image == null) {
			ImageDescriptor desc = ImageDescriptor.getMissingImageDescriptor();
			try {
				desc = ImageDescriptor.createFromURL(new URL(path));
				imageRegistry.put(path, desc);
				image = imageRegistry.get(path);
			} catch (MalformedURLException me) {
			}
		}
		
		return image;
	}
	
}
