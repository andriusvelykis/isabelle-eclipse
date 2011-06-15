package isabelle.eclipse.launch.tabs;

import isabelle.eclipse.launch.IsabelleLaunchImages;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;


public class LogicLabelProvider extends LabelProvider {
	
	public Image getImage(Object object) {
        Assert.isTrue(object instanceof String);

        return IsabelleLaunchImages.getImage(IsabelleLaunchImages.IMG_LOGIC);
    }

    public String getText(Object object) {
        Assert.isTrue(object instanceof String);
        return (String) object;
    }
}
