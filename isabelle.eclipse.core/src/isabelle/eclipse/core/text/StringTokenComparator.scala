package isabelle.eclipse.core.text

import org.eclipse.compare.rangedifferencer.IRangeComparator
import scala.annotation.tailrec

/** Range comparator for String tokens. To use with [[org.eclipse.compare.rangedifferencer.RangeDifferencer]]
  * to find differences between string token ranges (e.g. find word differences in text blocks).
  * 
  * @param tokens  a list of string tokens. They should not have gaps if representing a continuous text block,
  *                otherwise `tokenRangeContent()` will give inaccurate offsets.
  * @author Andrius Velykis
  */
class StringTokenComparator(val tokens: IndexedSeq[String]) extends IRangeComparator {
  
  override def getRangeCount(): Int = tokens.size

  override def rangesEqual(thisIndex: Int, other: IRangeComparator, otherIndex: Int): Boolean = other match {
    case other: StringTokenComparator => tokens(thisIndex) == other.tokens(otherIndex)
    // do not handle other cases - allow the exception
  }

  // never skip - this class should not be used with very long strings
  override def skipRangeComparison(length: Int, maxLength: Int, other: IRangeComparator): Boolean = false
  
  /** Retrieves the content represented by token range (as given from the RangeDifferencer).
    * @param start   start token
    * @param length  number of tokens from the start in the range
    * @return  range content as (offset, text)
    */
  def tokenRangeContent(start: Int, length: Int): (Int, String) = {
    
    // sum up the lengths of 'start' number of ranges
    val offset = tokens.take(start).map(_.length).sum
    
    // concat the text in the ranges from start
    val content = tokens.drop(start).take(length).mkString
    
    (offset, content)
  }
  
}

object StringTokenComparator {

  /** Creates a word-based range comparator for the given text block. */
  def wordComparator(text: String) =
    // split into words and whitespaces
    new StringTokenComparator(text.split("(?!^)\\b").toIndexedSeq)
}
