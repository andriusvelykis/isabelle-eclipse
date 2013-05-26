package isabelle.eclipse.ui.text

import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.rules.IToken
import org.eclipse.jface.text.rules.ITokenScanner
import org.eclipse.jface.text.rules.Token


/** Abstract token scanner for Stream-based token scanning. Implementing classes need to provide
  * a token stream for the given range.
  * <p>
  * The class provides functionality to cater for gaps between scanned tokens. If a gap is encountered,
  * it is filled with UNDEFINED token.
  * </p>
  * 
  * @author Andrius Velykis 
  */
trait AbstractTokenStreamScanner extends ITokenScanner {

  /** The stream of tokens for the scan range */
  private var tokenStream: Stream[TokenInfo] = Stream.empty
  
  /** Last token information to get offset/length */
  private var lastTokenInfo: TokenInfo = _
  
  override def setRange(document: IDocument, offset: Int, length: Int) {
    // mark the start of the range as the last token
    lastTokenInfo = undefinedToken(offset, 0)
    
    // construct a stream to read tokens
    val tokens = tokenStream(document, offset, length)
    tokenStream = normaliseTokenStream(tokens, offset)
  }
  
  /** Constructs a token stream for the given document range, which will then be queried for tokens */
  protected def tokenStream(document: IDocument, offset: Int, length: Int): Stream[TokenInfo]

  /** A convenience method to construct a stream with UNDEFINED token for the given range*/
  protected def undefinedStream(offset: Int, length: Int): Stream[TokenInfo] =
    Stream.cons(undefinedToken(offset, length), Stream.empty)

  protected def undefinedToken(offset: Int, length: Int) = TokenInfo(Token.UNDEFINED, offset, length)


  /**
   * Normalise token stream to ensure that the tokens are not overlapping,
   * otherwise exceptions occur in SWT.
   */
  private def normaliseTokenStream(tokens: Stream[TokenInfo],
                                   offset: Int): Stream[TokenInfo] = if (tokens.isEmpty) {
    Stream.empty
  } else {
    val nextToken = tokens.head
    val end = nextToken.end

    val newOffset = offset max nextToken.offset
    val newEnd = end max newOffset
    val newLength = newEnd - newOffset

    val normToken = nextToken.copy(offset = newOffset, length = newLength)

    normToken #:: normaliseTokenStream(tokens.tail, newEnd)
  }

    
  def nextToken(): IToken = {
    
    if (!tokenStream.isEmpty) {
      
      val nextToken = tokenStream.head
      
      // check if there is a gap between the next token and the last one
      val gapToken = if (nextToken.offset > lastTokenInfo.end) {
        // gap found - return undefined token for the gap
        undefinedToken(lastTokenInfo.end, nextToken.offset - lastTokenInfo.end)
      } else {
        // no gap - use the next token and update the remaining tokens
        tokenStream = tokenStream.tail
        nextToken
      }
      
      // mark the token info
      lastTokenInfo = gapToken
      lastTokenInfo.token
    } else {
      // no more tokens left - EOF
      Token.EOF
    }
  }

  def getTokenOffset() = lastTokenInfo.offset

  def getTokenLength() = lastTokenInfo.length
    
  protected case class TokenInfo(val token: IToken, val offset: Int, val length: Int) {
    def end = offset + length
  }
  
}

