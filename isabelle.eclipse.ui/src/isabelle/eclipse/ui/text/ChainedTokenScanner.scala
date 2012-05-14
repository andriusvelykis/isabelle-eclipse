package isabelle.eclipse.ui.text

import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.rules.ITokenScanner
import org.eclipse.jface.text.rules.IToken
import org.eclipse.jface.text.rules.Token


/** Token scanner that composes two token scanners in a chain and serves tokens from the
  * top scanner if it overlaps the bottom one. 
  * <p>
  * If the top scanner has UNDEFINED tokens, they are ignored, and corresponding bottom
  * scanner tokens are used in that gap.
  * </p>
  * 
  * @author Andrius Velykis
  */
class ChainedTokenScanner(private val top: ITokenScanner, private val bottom: ITokenScanner) 
  extends AbstractTokenStreamScanner {

  protected def tokenStream(document: IDocument, offset: Int, length: Int): Stream[TokenInfo] = {
    // update the scanners with the new range
    top.setRange(document, offset, length)
    bottom.setRange(document, offset, length)
    
    // treat scanners as streams and construct a composite stream from them
    tokenStream(tokenDefStream(top), tokenDefStream(bottom))
  }
  
  private def tokenStream(topStream: Stream[TokenInfo], bottomStream: Stream[TokenInfo]): Stream[TokenInfo] =
    if (topStream.isEmpty && bottomStream.isEmpty) {
      // both streams are empty - nothing left
      Stream.empty
    } else {

      val nextToken = if (bottomStream.isEmpty) {
        // only top remaining - use them
        topStream.head
      } else if (topStream.isEmpty) {
        // only bottom remaining - use them
        bottomStream.head
      } else {

        // both streams have elements. We need to determine which one comes next:
        // * the top element is first/equal to the bottom - use it
        // * the bottom element is first - take it (or part of it before the next top element)
        val topInfo = topStream.head
        val bottomInfo = bottomStream.head
        
        val nextOffset = scala.math.min(topInfo.offset, bottomInfo.offset)
        
        // check which stream element is closer
        if (bottomInfo.offset - nextOffset < topInfo.offset - nextOffset) {
          // the bottom stream is closer
          // take the bottom token but limited to top distance
          //    <--top-->
          // <--|-bottom---->
          if (topInfo.offset > bottomInfo.end) {
            // the whole bottom is before the next top
            //            <--top-->
            // <-bottom->|<--nextBottom?-->
            bottomInfo
          } else {
            // bottom is longer than top, so cut to the top offset
            //    <--top-->
            // <--|-bottom---->
            TokenInfo(bottomInfo.token, bottomInfo.offset, topInfo.offset - bottomInfo.offset)
          }
        } else {
          // top is closer/equal - take that one
          // <--top-->
          // <-bottom->
          topInfo
        }
      }
      
      val nextEnd = nextToken.end
      // drop both streams to the end offset of the next token
      val remainingTop = dropToOffset(topStream, nextEnd)
      val remainingBottom = dropToOffset(bottomStream, nextEnd)
      
      // construct the stream with the token at the start and remaining (dropped) streams
      Stream.cons(nextToken, tokenStream(remainingTop, remainingBottom))
    }
  
  private def dropToOffset(tokenStream : Stream[TokenInfo], offset: Int): Stream[TokenInfo] = {
    // drop tokens before the offset
    val droppedStream = tokenStream.dropWhile(info => (info.end <= offset))
    
    // an overlapping token may still be at the front of the stream,
    // cut the token to the offset if necessary
    if (!droppedStream.isEmpty) {
      val info = droppedStream.head
      if (info.offset < offset) {
        // found overlapping token: cut it
        // <--|--token--><--nextToken?--> 
        //    | offset
        val cutInfo = TokenInfo(info.token, offset, info.end - offset)
        Stream.cons(cutInfo, droppedStream.tail)
      } else {
        // nothing to cut
        droppedStream
      }
    } else {
      droppedStream
    }
  }
  
  private def tokenDefStream(scanner: ITokenScanner) =
    tokenStream(scanner) filter (info => !info.token.isUndefined)
  
  private def tokenStream(scanner: ITokenScanner): Stream[TokenInfo] = {
    val info = tokenInfo(scanner)
    
    if (info.token.isEOF) {
      Stream.empty
    } else {
      Stream.cons(info, tokenStream(scanner))
    }
  }
  
  private def tokenInfo(scanner: ITokenScanner): TokenInfo = 
    TokenInfo(scanner.nextToken, scanner.getTokenOffset(), scanner.getTokenLength())

}
