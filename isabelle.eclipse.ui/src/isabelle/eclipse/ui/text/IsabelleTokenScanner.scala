package isabelle.eclipse.ui.text

import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.rules.IToken
import org.eclipse.jface.text.rules.Token

import isabelle.Outer_Syntax
import isabelle.Scan
import isabelle.Session
import isabelle.eclipse.ui.editors.TheoryEditor

/** A token scanner that utilises Isabelle/Scala tokeniser. Requires Isabelle Session to be loaded,
  * which is retrieved via the editor.
  * 
  * @author Andrius Velykis
  */
class IsabelleTokenScanner(val editor: TheoryEditor) extends AbstractTokenStreamScanner {

  private def syntax: Option[Outer_Syntax] = {
    val isabelleModel = Option(editor.getIsabelleModel)
    
    val session = isabelleModel.map(_.session).filter(_.is_ready)
    session.map(_.recent_syntax)
  }

  protected def tokenStream(document: IDocument, offset: Int, length: Int): Stream[TokenInfo] =
    syntax match {
      case None => {
        // syntax (Isabelle session) is not available, so cannot tokenise
        // use stream with undefined token
        undefinedStream(offset, length)
      }

      case Some(syntax) => {
        // use syntax scanner to get Isabelle tokens
        // TODO do a line-based scanning?
        val source = document.get(offset, length)
        val (tokens, _) = syntax.scan_context(source, Scan.Finished)
        
        // map the token list as the token stream
        tokenStream(syntax, tokens, offset)
      }
    }
  
  /** Constructs a token stream from the token list */
  private def tokenStream(syntax: Outer_Syntax, tokens: List[isabelle.Token], offset: Int): Stream[TokenInfo] =
    if (tokens.isEmpty) {
      Stream.empty
    } else {
      
      // map first token to TokenInfo
      val token = tokens.head
      val length = token.source.length();
      val tokenInfo = TokenInfo(getTokenAll(syntax, token), offset, length)
      // adjust next token offset
      val nextOffset = offset + length
      
      Stream.cons(tokenInfo, tokenStream(syntax, tokens.tail, nextOffset))
    }
  
  /** Handles SPACE and UNPARSED token kinds as core JFace tokens, delegates everything else to #getToken(Token) */
  protected def getTokenAll(syntax: Outer_Syntax, token: isabelle.Token): IToken = {
    import isabelle.Token.Kind._
    
    token.kind match {
      case SPACE => Token.WHITESPACE
      case UNPARSED => Token.UNDEFINED
      case _ => getToken(syntax, token)
    }
  }

  /** Allow subclasses to specify different JFace tokens for Isabelle tokens, e.g. with colours, etc. */
  protected def getToken(syntax: Outer_Syntax, token: isabelle.Token): IToken = new Token(token)

}
