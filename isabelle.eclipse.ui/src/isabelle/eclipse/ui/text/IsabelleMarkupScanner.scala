package isabelle.eclipse.ui.text

import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.rules.{IToken, Token}

import isabelle.{Markup, Text, XML}
import isabelle.Document.Snapshot
import isabelle.eclipse.ui.preferences.IsabelleMarkupToSyntaxClass


/**
 * A markup scanner that retrieves markup information from document snapshot.
 *
 * @author Andrius Velykis
 */
class IsabelleMarkupScanner(snapshot: => Option[Snapshot]) extends AbstractTokenStreamScanner {

  protected def tokenStream(document: IDocument, offset: Int, length: Int): Stream[TokenInfo] =
    snapshot match {
      case None => {
        // snapshot (Isabelle session) is not available, so cannot get the markup
        // use stream with undefined token
        undefinedStream(offset, length)
      }

      case Some(snapshot) => {
        // create markup stream to get tokens
        val range = Text.Range(offset, offset + length)
        val markups = IsabelleMarkupToSyntaxClass.markups
        
        val markupInfos =
          snapshot.cumulate_markup[Option[IToken]](
            range, None, Some(markups), _ =>
            {
              // need additional check if the markup is valid, otherwise we get clashing markups
              // for the same range and some colours are not defined
              case (_, MarkupName(markup)) if (markups.contains(markup)) => Some(getToken(markup))
            })
        
        // map markup infos to TokenInfo stream 
        markupInfos map tokenInfo
      }
    }

  private object MarkupName {
    def unapply(info: Any): Option[String] =
      info match {
//        case Text.Info(_, XML.Elem(Markup.Entity(kind, _), _)) => Some(kind)
        case Text.Info(_, XML.Elem(Markup(m, _), _)) => Some(m)
        case _ => None
      }
  }
  
  private def tokenInfo(tokenInfo: Text.Info[Option[IToken]]) = 
    TokenInfo(tokenInfo.info getOrElse Token.UNDEFINED, 
        tokenInfo.range.start, tokenInfo.range.stop - tokenInfo.range.start)

  /** Allow subclasses to specify different JFace tokens for Isabelle markup, e.g. with colours, etc. */
  protected def getToken(markupType: String): IToken = new Token(markupType)

}
