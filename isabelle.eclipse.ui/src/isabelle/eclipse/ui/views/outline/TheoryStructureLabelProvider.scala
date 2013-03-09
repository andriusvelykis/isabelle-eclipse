package isabelle.eclipse.ui.views.outline

import org.eclipse.jface.resource.ResourceManager
import org.eclipse.jface.viewers.LabelProvider
import org.eclipse.swt.graphics.Image

import isabelle.Library
import isabelle.Thy_Syntax.Structure
import isabelle.eclipse.ui.internal.IsabelleImages


/**
 * Label provider for Thy_Syntax.Structure entries.
 * 
 * @author Andrius Velykis
 */
/* Adapted from Isabelle_Sidekick_Structure */
class TheoryStructureLabelProvider(resourceManager: ResourceManager) extends LabelProvider {
  
  override def getImage(obj: AnyRef): Image =
    resourceManager.createImageWithDefault(IsabelleImages.OUTLINE_ITEM)


  override def getText(obj: AnyRef): String = obj match {
    case entry: TheoryStructureEntry => entry.entry match {
      case Structure.Block(name, body) => Library.first_line(name)
      case Structure.Atom(command) => command.name
    }
    case _ => super.getText(obj)
  }
}
