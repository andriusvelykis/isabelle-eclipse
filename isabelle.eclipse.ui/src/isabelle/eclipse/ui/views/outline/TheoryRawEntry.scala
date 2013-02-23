package isabelle.eclipse.ui.views.outline

import isabelle.{Command, Markup_Tree, Text, XML}


/**
 * A value class encapsulating an Isabelle theory raw markup outline node.
 * 
 * @author Andrius Velykis
 */
/* Adapted from Isabelle_Sidekick_Raw */
case class TheoryRawEntry(val markup: List[XML.Elem],
                          val rangeInCmd: Text.Range,
                          tree: Markup_Tree,
                          val info: TheoryRawEntry.Info,
                          val parent: Option[TheoryRawEntry] = None) {

  lazy val children: Iterable[TheoryRawEntry] = TheoryRawEntry.branches(tree, info, Some(this))

  lazy val range = rangeInCmd + info.commandStart

  lazy val content = info.contentRenderer(this)

  lazy val tooltip = info.tooltipRenderer(this)

}

object TheoryRawEntry {

  case class Info(val command: Command,
                  val commandStart: Int,
                  val contentRenderer: TheoryRawEntry => String,
                  val tooltipRenderer: TheoryRawEntry => String)

  def branches(tree: Markup_Tree,
               info: Info,
               parent: Option[TheoryRawEntry] = None): Iterable[TheoryRawEntry] =
    tree.branches map {
      case (_, entry) => TheoryRawEntry(
        entry.markup,
        entry.range,
        entry.subtree,
        info,
        parent)
    }

}
