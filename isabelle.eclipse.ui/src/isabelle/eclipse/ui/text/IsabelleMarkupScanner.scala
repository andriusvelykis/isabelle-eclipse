package isabelle.eclipse.ui.text

import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.rules.IToken
import org.eclipse.jface.text.rules.Token

import isabelle.Document.Snapshot
import isabelle.Isabelle_Markup
import isabelle.Markup
import isabelle.Text
import isabelle.XML
import isabelle.eclipse.ui.editors.TheoryEditor


/** A markup scanner that retrieves markup information from document snapshot. 
  * Requires Isabelle Session to be loaded, which is retrieved via the editor.
  * 
  * @author Andrius Velykis
  */
class IsabelleMarkupScanner(val editor: TheoryEditor) extends AbstractTokenStreamScanner {

  private def snapshot: Option[Snapshot] = Option(editor.getIsabelleModel).map(_.snapshot)
  
  protected def tokenStream(document: IDocument, offset: Int, length: Int): Stream[TokenInfo] =
    snapshot match {
      case None => {
        // snapshot (Isabelle session) is not available, so cannot get the markup
        // use stream with undefined token
        undefinedStream(offset, length)
      }

      case Some(snapshot) => {
        // create markup stream to get tokens
        val markupInfos =
          snapshot.cumulate_markup[Option[IToken]](
            Text.Range(offset, offset + length), None, None,
            {
              case (_, MarkupName(markup)) => Some(getToken(markup))
            })
        
        // map markup infos to TokenInfo stream 
        markupInfos map tokenInfo
      }
    }

  private object MarkupName {
    def unapply(info: Any): Option[String] =
      info match {
        case Text.Info(_, XML.Elem(Isabelle_Markup.Entity(kind, _), _)) => Some(kind)
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
