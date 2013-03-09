package isabelle.eclipse.ui.text

import org.eclipse.jface.text.rules.IToken

import isabelle.{Markup, Protocol, Text}
import isabelle.Command
import isabelle.Document.Snapshot
import isabelle.eclipse.ui.preferences.IsabelleMarkupToSyntaxClass


/**
 * A markup scanner that retrieves markup information from document snapshot.
 * 
 * A specialised version for action markups, e.g. "sendback".
 *
 * @author Andrius Velykis
 */
class IsabelleActionMarkupScanner(snapshot: => Option[Snapshot])
    extends AbstractMarkupScanner(snapshot) {


  override val supportedMarkups: Set[String] =
    Set(Markup.BROWSER, Markup.GRAPHVIEW, Markup.SENDBACK, Markup.DIALOG)


  override def markupMatch(commandState: Command.State)
      : PartialFunction[(Option[IToken], Text.Markup), Option[IToken]] = {
    case (_, Text.Info(info_range, Protocol.Dialog(_, serial, result))) =>
      commandState.results.get(serial) match {
        // this option was selected
        case Some(Protocol.Dialog_Result(res)) if res == result =>
          Some(getToken(IsabelleMarkupToSyntaxClass.DIALOG_SELECTED))

        // nothing selected yet - render as dialog
        case None =>
          Some(getToken(Markup.DIALOG))

        // something selected, but not this one - remove special rendering
        case _ => None
      }

    case (_, MarkupName(name))
      if name == Markup.BROWSER || name == Markup.GRAPHVIEW || name == Markup.SENDBACK => 
        Some(getToken(name))
  }

}
