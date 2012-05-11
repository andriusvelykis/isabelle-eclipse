package isabelle.eclipse.ui;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;


public class IsabelleEclipseImages {

	/* Declare Common paths */
	private static URL ICON_BASE_URL= null;

	static {
		String pathSuffix = "icons/"; //$NON-NLS-1$	
		ICON_BASE_URL= IsabelleEclipsePlugin.getDefault().getBundle().getEntry(pathSuffix);
	}
	
	public static final String IMG_RAW_OUTPUT_CONSOLE = ICON_BASE_URL + "isabelle.png";
	public static final String IMG_CONTENT_ASSIST = ICON_BASE_URL + "isabelle.png";
	public static final String IMG_OUTLINE_ITEM = ICON_BASE_URL + "isabelle.png";
	
	/**
	 * Returns the <code>Image<code> identified by the given path,
	 * or <code>null</code> if it does not exist.
	 */
	public static Image getImage(String path) {
		ImageRegistry imageRegistry = IsabelleEclipsePlugin.getDefault().getImageRegistry();
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
	
	public static ImageDescriptor getImageDescriptor(String path) {
		return IsabelleEclipsePlugin.getDefault().getImageRegistry().getDescriptor(path);
	}
	
}
