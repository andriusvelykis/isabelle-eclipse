package isabelle.eclipse.ui.text

import org.eclipse.jface.text.rules.IToken

import isabelle.Command
import isabelle.Document.Snapshot
import isabelle.Text
import isabelle.eclipse.ui.preferences.IsabelleMarkupToSyntaxClass


/**
 * A markup scanner that retrieves markup information from document snapshot.
 *
 * @author Andrius Velykis
 */
class IsabelleMarkupScanner(snapshot: => Option[Snapshot], markups: Set[String])
    extends AbstractMarkupScanner(snapshot) {

  override val supportedMarkups: Set[String] = markups

  override def markupMatch(state: Command.State)(
                            token: IToken,
                            markup: Text.Markup): Option[IToken] = (token, markup) match {
    
    // need additional check if the markup is valid, otherwise we get clashing markups
    // for the same range and some colours are not defined
    case (_, MarkupName(markup)) if (supportedMarkups.contains(markup)) => Some(getToken(markup))

    case _ => None
  }

}
