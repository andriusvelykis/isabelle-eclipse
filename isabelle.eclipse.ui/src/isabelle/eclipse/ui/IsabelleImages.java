package isabelle.eclipse.ui;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;


public class IsabelleImages {

	/* Declare Common paths */
	private static URL ICON_BASE_URL= null;

	static {
		String pathSuffix = "icons/"; //$NON-NLS-1$	
		ICON_BASE_URL= IsabelleUIPlugin.getDefault().getBundle().getEntry(pathSuffix);
	}
	
	public static final String IMG_RAW_OUTPUT_CONSOLE = ICON_BASE_URL + "isabelle.png";
	public static final String IMG_CONTENT_ASSIST = ICON_BASE_URL + "isabelle.png";
	public static final String IMG_OUTLINE_ITEM = ICON_BASE_URL + "isabelle.png";
	
	/**
	 * Returns the <code>Image<code> identified by the given path,
	 * or <code>null</code> if it does not exist.
	 */
	public static Image getImage(String path) {
		ImageRegistry imageRegistry = IsabelleUIPlugin.getDefault().getImageRegistry();
		Image image = imageRegistry.get(path);
		if (image == null) {
			getImageDescriptor(path);
			image = imageRegistry.get(path);
		}
		
		return image;
	}
	
	/**
	 * Returns the <code>ImageDescriptor<code> identified by the given path,
	 * or <code>null</code> if it does not exist.
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		ImageRegistry imageRegistry = IsabelleUIPlugin.getDefault().getImageRegistry();
		ImageDescriptor desc = imageRegistry.getDescriptor(path);
		if (desc == null) {
			desc = ImageDescriptor.getMissingImageDescriptor();
			try {
				desc = ImageDescriptor.createFromURL(new URL(path));
				imageRegistry.put(path, desc);
			} catch (MalformedURLException me) {
			}
		}
		
		return desc;
	}
	
}
