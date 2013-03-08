package isabelle.eclipse.ui.views.outline

import org.eclipse.jface.resource.ResourceManager
import org.eclipse.jface.viewers.LabelProvider
import org.eclipse.swt.graphics.Image

import isabelle.{Library, Pretty}
import isabelle.eclipse.ui.internal.IsabelleImages


/**
 * Label provider for raw XML markup structure of an Isabelle theory file.
 * 
 * @author Andrius Velykis
 */
/* Adapted from Isabelle_Sidekick_Raw */
class TheoryRawLabelProvider(resourceManager: ResourceManager) extends LabelProvider {
  
  override def getImage(obj: AnyRef): Image =
    resourceManager.createImageWithDefault(IsabelleImages.OUTLINE_ITEM)


  override def getText(obj: AnyRef): String = obj match {
    case entry: TheoryRawEntry => entry.content
    case _ => super.getText(obj)
  }
}


object TheoryRawLabelProvider {

  def rawContent(entry: TheoryRawEntry): String =
    entry.info.command.source(entry.rangeInCmd).replace('\n', ' ')

  def rawTooltip(entry: TheoryRawEntry): String =
    Pretty.formatted(Library.separate(Pretty.FBreak, entry.markup), margin = 40).mkString
  
}
