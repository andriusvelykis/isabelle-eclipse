package isabelle.eclipse.ui.text

import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.rules.{IToken, ITokenScanner}


/**
 * Token scanner that composes two token scanners in a chain and can merge the tokens if the
 * top one overlaps the bottom one.
 *
 * @author Andrius Velykis
 */
class ChainedTokenScanner(top: ITokenScanner, bottom: ITokenScanner,
                          merge: ((IToken, IToken) => IToken) = TokenUtil.Merge.takeTopToken)
    extends AbstractTokenStreamScanner {

  protected def tokenStream(document: IDocument, offset: Int, length: Int): Stream[TokenInfo] = {
    // update the scanners with the new range
    top.setRange(document, offset, length)
    bottom.setRange(document, offset, length)
    
    // treat scanners as streams and construct a composite stream from them
    tokenStream(tokenDefStream(top), tokenDefStream(bottom))
  }

  private def tokenStream(topStream: Stream[TokenInfo], bottomStream: Stream[TokenInfo]): Stream[TokenInfo] =
    (topStream.headOption, bottomStream.headOption) match {

      // both streams are empty - nothing left
      case (None, None) => Stream.empty

      // only top remaining - use them
      case (Some(top), None) => Stream.cons(top, tokenStream(topStream.tail, bottomStream))

      // only bottom remaining - use them
      case (None, Some(bottom)) => Stream.cons(bottom, tokenStream(topStream, bottomStream.tail))

      case (Some(top), Some(bottom)) => {
        // both streams have elements. We need to determine which one comes next:
        // * if one of the elements is before another, take it whole
        // * if one of the elements is partially before another, take the partial bit as a new token
        // * if both elements start at the same time, merge them
        //
        // Illustrations of some of these cases:
        //
        // The whole bottom is before the next top
        //            <--top-->
        // <-bottom->|<--nextBottom?-->
        //
        // Bottom is longer than top, so cut to the top offset
        //    <--top-->
        // <--|-bottom---->
        //
        // Both start at the same place - merge and cut to whichever is shorter
        // <--top-->
        // <-bottom-|-->

        // the end of token is either the first end of the current token, or the start of the other
        // token, whichever is smaller.
        // this means that one of the tokens can be fully before another starts, or they can overlap
        lazy val tokenEnd = (top.end min bottom.end)
        lazy val overlapEnd = (top.offset max bottom.offset) min tokenEnd

        val nextToken =
          if (top.offset == bottom.offset) {
            // both tokens start at the same place
            // merge them and use the range to whichever ends first
            TokenInfo(merge(top.token, bottom.token), top.offset, tokenEnd - top.offset)
          } else {

            // one of the tokens is before the other
            // select the token and cut to the overlap (note that a whole token is taken if no overlap)
            val (token, start) =
              if (top.offset < bottom.offset) (top.token, top.offset)
              else (bottom.token, bottom.offset)
            TokenInfo(token, start, overlapEnd - start)
          }

        val nextEnd = nextToken.end
        // drop both streams to the end offset of the next token
        val remainingTop = dropToOffset(topStream, nextEnd)
        val remainingBottom = dropToOffset(bottomStream, nextEnd)

        // construct the stream with the token at the start and remaining (dropped) streams
        Stream.cons(nextToken, tokenStream(remainingTop, remainingBottom))
      }
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
