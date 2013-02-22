package isabelle.eclipse.ui.views.outline

import isabelle.Outer_Syntax
import isabelle.Thy_Syntax.Structure

/**
 * A value class encapsulating an Isabelle Thy_Syntax.Structure outline node.
 * 
 * Displays block-level elements and heading-level commands in the tree.
 * 
 * @author Andrius Velykis
 */
/* Adapted from Isabelle_Sidekick_Structure */
case class TheoryStructureEntry(syntax: Outer_Syntax,
                                val entry: Structure.Entry,
                                val offset: Int,
                                val parent: Option[TheoryStructureEntry] = None) {

  lazy val children: List[TheoryStructureEntry] =
    entry match {
      case Structure.Block(name, body) => {

        val childOffsets = (body scanLeft offset) { (i, e) =>
          i + e.length
        }

        val allChildren = (body zip childOffsets) map {
          case (e, o) => TheoryStructureEntry(syntax, e, o, Some(this))
        }

        // exclude non-structure entries
        val structureChildren = allChildren filter (e => isStructureNode(e.entry))
        structureChildren
      }
      case _ => Nil
    }

  private def isStructureNode(entry: Structure.Entry) = entry match {
    case Structure.Block(_, _) => true
    case Structure.Atom(command)
      if command.is_command && syntax.heading_level(command).isEmpty => true
    case _ => false
  }
}

object TheoryStructureEntry {

  def apply(syntax: Outer_Syntax, entry: Structure.Entry): TheoryStructureEntry =
    TheoryStructureEntry(syntax, entry, 0)
  
}
