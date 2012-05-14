package isabelle.eclipse.ui.text

import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.Region
import org.eclipse.jface.text.rules.IToken
import org.eclipse.jface.text.rules.ITokenScanner
import org.eclipse.jface.text.rules.Token


/** A token scanner that returns a single token for the whole range. Useful for partition ranges.
  * 
  * @author Andrius Velykis
  */
class SingleTokenScanner() extends ITokenScanner {

  private var lastRegion: IRegion = _
  private var consumed = true

  def setRange(document: IDocument, offset: Int, length: Int) {
    // set the region and allow consumption
    lastRegion = new Region(offset, length)
    consumed = false
  }

  def nextToken(): IToken = if (consumed) {
    Token.EOF
  } else {
    // consume the region
    consumed = true
    // return default token
    getToken
  } 

  def getTokenOffset() = lastRegion.getOffset()

  def getTokenLength() = lastRegion.getLength()
  
  /** Allow subclasses to provide the token */
  protected def getToken: IToken = Token.UNDEFINED

}
