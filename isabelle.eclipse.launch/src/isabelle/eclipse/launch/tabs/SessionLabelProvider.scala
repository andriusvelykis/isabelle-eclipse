package isabelle.eclipse.launch.tabs

import org.eclipse.jface.resource.ResourceManager
import org.eclipse.jface.viewers.LabelProvider
import org.eclipse.swt.graphics.Image

import isabelle.eclipse.launch.IsabelleLaunchImages


/**
 * Basic label provider for Isabelle session selector.
 * 
 * @author Andrius Velykis
 */
class SessionLabelProvider(resourceManager: ResourceManager) extends LabelProvider {
  
  override def getImage(obj: AnyRef): Image =
    resourceManager.createImageWithDefault(IsabelleLaunchImages.SESSION)


  override def getText(obj: AnyRef): String = obj match {
    case s: String => s
    case _ => String.valueOf(obj)
  }
  
}
