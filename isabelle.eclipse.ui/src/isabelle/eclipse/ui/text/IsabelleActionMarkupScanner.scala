package isabelle.eclipse.ui.text

import org.eclipse.jface.text.rules.IToken

import isabelle.{Markup, Protocol, Text}
import isabelle.Command
import isabelle.Document.Snapshot


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

    case (_, Text.Info(info_range, elem @ Protocol.Dialog(_, serial, _))) 
      if !commandState.results.defined(serial) =>
        Some(getToken(Markup.DIALOG))

    case (_, MarkupName(name))
      if name == Markup.BROWSER || name == Markup.GRAPHVIEW || name == Markup.SENDBACK => 
        Some(getToken(name))
  }

}
