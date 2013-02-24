package isabelle.eclipse.ui.editors

import org.eclipse.jface.resource.ResourceManager
import org.eclipse.jface.text.source.ISharedTextColors
import org.eclipse.swt.graphics.{Color, RGB}


/**
 * Shared text colors that use underlying ResourceManager for caching the colors.
 *
 * @author Andrius Velykis
 */
class ManagedTextColors(resourceManager: ResourceManager) extends ISharedTextColors {

  override def getColor(rgb: RGB): Color = resourceManager.createColor(rgb)

  // no need to dispose, since the resource manager handles everything
  override def dispose() {}

}
