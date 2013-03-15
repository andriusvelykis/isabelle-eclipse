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

  private val cmdImages = Map(
    "qed" -> IsabelleImages.SUCCESS,
    "done" -> IsabelleImages.SUCCESS,
    "by" -> IsabelleImages.SUCCESS,
    "apply" -> IsabelleImages.COMMAND_APPLY,
    "proof" -> IsabelleImages.COMMAND_PROOF
    ).withDefaultValue(IsabelleImages.ISABELLE_ITEM)


  override def getImage(obj: AnyRef): Image = {

    val imgDesc = obj match {
      case TheoryStructureEntry(_, Structure.Block(nameText, _), _, _) => {
        val name = nameText.trim
        
        if (name.startsWith("lemma") || name.startsWith("theorem")) {
          IsabelleImages.LEMMA
        } else if (name.startsWith("text")) {
          IsabelleImages.TEXT
        } else if (name.startsWith("theory")) {
          IsabelleImages.ISABELLE_FILE
        } else {
          IsabelleImages.HEADING
        }
      }

      case TheoryStructureEntry(_, Structure.Atom(command), _, _) => cmdImages(command.name)

      case _ => IsabelleImages.ISABELLE_ITEM
    }

    resourceManager.createImageWithDefault(imgDesc)
  }


  override def getText(obj: AnyRef): String = obj match {
    case entry: TheoryStructureEntry => entry.entry match {
      case Structure.Block(name, body) => Library.first_line(name)
      case Structure.Atom(command) => command.name
    }
    case _ => super.getText(obj)
  }
}
