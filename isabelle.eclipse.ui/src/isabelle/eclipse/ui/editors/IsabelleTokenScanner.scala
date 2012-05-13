package isabelle.eclipse.ui.editors

import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.rules.IPartitionTokenScanner
import org.eclipse.jface.text.rules.ITokenScanner
import org.eclipse.jface.text.rules.IToken
import org.eclipse.jface.text.rules.Token

import isabelle.Outer_Syntax
import isabelle.Scan
import isabelle.Scan.Context
import isabelle.Session

/** A token scanner that utilises Isabelle/Scala tokeniser. Requires Isabelle Session to be loaded,
  * which is retrieved via the editor.
  * 
  * @author Andrius Velykis
  */
class IsabelleTokenScanner(val editor: TheoryEditor) extends ITokenScanner {

  /** The outstanding Isabelle tokens of the parsed range */
  private var tokens: List[isabelle.Token] = Nil

  /** Parsing context after the range was parsed 
    * (not used at the moment - left if line-based parsing will be implemented)
    */
  private var context: Context = _
  
  private var lastTokenOffset: Int = _
  private var lastTokenLength: Int = _

  private def syntax: Option[Outer_Syntax] = {
    val isabelleModel = editor.getIsabelleModel();
    if (isabelleModel != null && isabelleModel.getSession().is_ready) {
      Some(isabelleModel.getSession().current_syntax())
    } else {
      None
    }
  }

  def setRange(document: IDocument, offset: Int, length: Int) {

    val source = document.get(offset, length)

    // reset last offset to the start of the range
    this.lastTokenOffset = offset
    this.lastTokenLength = 0

    val (tokens, context) = syntax match {
      case None => {
        // syntax (Isabelle session) is not available, so cannot tokenise
        // create a token with null type to indicate lack of Isabelle session
        (List(isabelle.Token(null, source)), null)
      }

      case Some(syntax) => {
        // use syntax scanner to get Isabelle tokens
        // TODO do a line-based scanning?
        syntax.scan_context(source, Scan.Finished)
      }
    }

    this.tokens = tokens
    this.context = context
  }

  def nextToken(): IToken = {

    // check the outstanding token list - if there are tokens, return them
    val (token, length, rest) = tokens match {
      case token :: rest => {
        
        // tokens are available - check if it not the undefined one
        val t = if (token.kind == null) {
          // undefined token
          Token.UNDEFINED
        } else {
          getToken(token)
        }

        (t, token.source.length, rest)
      }
      // no tokens available - signal EOF
      case Nil => (Token.EOF, 0, Nil)
    }

    // mark the remaining tokens
    this.tokens = rest
    // adjust the offset with the last length - it will be the new offset
    this.lastTokenOffset += lastTokenLength
    // set the new length
    this.lastTokenLength = length
    token
  }

  def getTokenOffset() = lastTokenOffset

  def getTokenLength() = lastTokenLength

  /** Allow subclasses to specify different JFace tokens for Isabelle tokens, e.g. with colours, etc. */
  protected def getToken(token: isabelle.Token): IToken = new Token(token)

}
