package isabelle.eclipse.core.text

/** Utilities to perform `diff` on text: find differences between two strings and calculate replacements
  * to bring the 'old' one to match the 'new'.
  * 
  * @author Andrius Velykis 
  */
object DiffUtils {

  /** Finds differences between the given string arguments. Performs word-based matching with additional
    * checks for common word prefix/suffix.
    * 
    * @param oldText  the 'old' text
    * @param newText  the 'new' text
    * @return  list of differences between the texts as (offsetInOld, oldTextDiff, offsetInNew, newTextDiff).
    *          The differences are based on 'old' text parameters and indicates the offset and
    *          string to replace to get the 'new' text. 
    */
  def diff(oldText: String, newText: String): List[(Int, String, Int, String)] = {

    import org.eclipse.compare.rangedifferencer.RangeDifferencer
    import StringTokenComparator._

    // use word comparators
    val oldComp = wordComparator(oldText)
    val newComp = wordComparator(newText)

    // use Eclipse's diff utilities
    val diffs = RangeDifferencer.findDifferences(oldComp, newComp);

    // for each range diff, resolve the actual text diffs
    diffs.toList map { diff =>
      val (oldOffset, oldContent) = oldComp.tokenRangeContent(diff.leftStart, diff.leftLength)
      val (newOffset, newContent) = newComp.tokenRangeContent(diff.rightStart, diff.rightLength)

      // words are different - try the difference further by checking for common prefix/suffix
      val (oldOffsetInWord, oldWordDiff, newOffsetInWord, newWordDiff) = diffWord(oldContent, newContent)
      // adjust the word offset
      (oldOffset + oldOffsetInWord, oldWordDiff, newOffset + newOffsetInWord, newWordDiff)
    }
  }
  
  /** Finds difference between two words. Returns the non-matching middle of the two words by 
    * checking if the words have common prefix or suffix. No further `diff` is performed.
    */
  def diffWord(oldText: String, newText: String): (Int, String, Int, String) = {

    def dropCommonPrefix(diff: (Int, String, Int, String)): (Int, String, Int, String) = {
      // unpack tuple
      val (oldOffset, oldText, newOffset, newText) = diff
      val prefix = longestCommonPrefix(oldText, newText)
      if (prefix > 0) {
        // found common prefix - adjust the offset and drop the prefix
        (oldOffset + prefix, oldText.substring(prefix), newOffset + prefix, newText.substring(prefix))
      } else {
        // nothing in common - return the originals
        (oldOffset, oldText, newOffset, newText)
      }
    }

    def dropCommonSuffix(diff: (Int, String, Int, String)): (Int, String, Int, String) = {
      val (oldOffset, oldText, newOffset, newText) = diff
      val suffix = longestCommonPrefix(oldText.reverse, newText.reverse)
      
      def dropRight(str: String, n: Int) = str.substring(0, str.length - n)

      if (suffix > 0) {
        // found common suffix - drop it from the strings
        (oldOffset, dropRight(oldText, suffix), newOffset, dropRight(newText, suffix))
      } else {
        // nothing in common - return the originals
        (oldOffset, oldText, newOffset, newText)
      }
    }

    // check for common prefix/suffix
    dropCommonSuffix(dropCommonPrefix(0, oldText, 0, newText))
  }
  
  /** Finds the length of the longest common prefix of both arguments. */
  def longestCommonPrefix(a: String, b: String): Int = {
    var same = true
    var i = 0
    while (same && i < math.min(a.length, b.length)) {
      if (a.charAt(i) != b.charAt(i)) {
        same = false
      } else {
        i += 1
      }
    }
    i
  }
  
}
