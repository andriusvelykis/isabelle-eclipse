package isabelle.eclipse.ui.text

import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.rules.{IToken, Token}

import isabelle.{Markup, Text, XML}
import isabelle.Command
import isabelle.Document.Snapshot


/**
 * An abstract markup scanner that retrieves markup information from document snapshot.
 * 
 * The markups of interest need to be provided by implementing classes.
 *
 * @author Andrius Velykis
 */
abstract class AbstractMarkupScanner(snapshot: => Option[Snapshot])
    extends AbstractTokenStreamScanner {

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
        val markups = supportedMarkups
        
        val markupInfos = snapshot.cumulate_markup(range, None, Some(markups), markupMatch)
        
        // map markup infos to TokenInfo stream 
        markupInfos map tokenInfo
      }
    }

  protected object MarkupName {
    def unapply(info: Any): Option[String] =
      info match {
//        case Text.Info(_, XML.Elem(Markup.Entity(kind, _), _)) => Some(kind)
        case Text.Info(_, XML.Elem(Markup(m, _), _)) => Some(m)
        case _ => None
      }
  }
  
  protected def supportedMarkups: Set[String]
  
  protected def markupMatch(state: Command.State): 
    PartialFunction[(Option[IToken], Text.Markup), Option[IToken]]
  
  private def tokenInfo(tokenInfo: Text.Info[Option[IToken]]) = 
    TokenInfo(tokenInfo.info getOrElse Token.UNDEFINED, 
        tokenInfo.range.start, tokenInfo.range.stop - tokenInfo.range.start)

  /** Allow subclasses to specify different JFace tokens for Isabelle markup, e.g. with colours, etc. */
  protected def getToken(markupType: String): IToken = new Token(markupType)

}
