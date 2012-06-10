package isabelle.eclipse.core.text

/** Utilities to perform `diff` on text: find differences between two strings and calculate replacements
  * to bring the one of the strings to match the other one.
  * 
  * @author Andrius Velykis 
  */
object DiffUtils {

  /** Finds differences between the given string arguments. Performs word-based matching with additional
    * checks for common word prefix/suffix.
    * 
    * @param left  first text to compare
    * @param right  second text to compare
    * @return  list of differences between the texts as (offsetInLeft, leftTextDiff, offsetInRight, rightTextDiff).
    */
  def diff(left: String, right: String): List[(Int, String, Int, String)] = {

    import org.eclipse.compare.rangedifferencer.RangeDifferencer
    import StringTokenComparator._

    // use word comparators
    val leftComp = wordComparator(left)
    val rightComp = wordComparator(right)

    // use Eclipse's diff utilities
    val diffs = RangeDifferencer.findDifferences(leftComp, rightComp);

    // for each range diff, resolve the actual text diffs
    diffs.toList map { diff =>
      val (leftOffset, leftContent) = leftComp.tokenRangeContent(diff.leftStart, diff.leftLength)
      val (rightOffset, rightContent) = rightComp.tokenRangeContent(diff.rightStart, diff.rightLength)

      // words are different - try the difference further by checking for common prefix/suffix
      val (leftWordOffset, leftWordDiff, rightWordOffset, rightWordDiff) = diffWord(leftContent, rightContent)
      // adjust the word offset
      (leftOffset + leftWordOffset, leftWordDiff, rightOffset + rightWordOffset, rightWordDiff)
    }
  }
  
  /** Finds difference between two words. Returns the non-matching middle of the two words by 
    * checking if the words have common prefix or suffix. No further `diff` is performed.
    */
  def diffWord(left: String, right: String): (Int, String, Int, String) = {

    def dropCommonPrefix(diff: (Int, String, Int, String)): (Int, String, Int, String) = {
      // unpack tuple
      val (leftOffset, left, rightOffset, right) = diff
      val prefix = longestCommonPrefix(left, right)
      if (prefix > 0) {
        // found common prefix - adjust the offset and drop the prefix
        (leftOffset + prefix, left.substring(prefix), rightOffset + prefix, right.substring(prefix))
      } else {
        // nothing in common - return the originals
        (leftOffset, left, rightOffset, right)
      }
    }

    def dropCommonSuffix(diff: (Int, String, Int, String)): (Int, String, Int, String) = {
      val (leftOffset, left, rightOffset, right) = diff
      val suffix = longestCommonPrefix(left.reverse, right.reverse)
      
      def dropRight(str: String, n: Int) = str.substring(0, str.length - n)

      if (suffix > 0) {
        // found common suffix - drop it from the strings
        (leftOffset, dropRight(left, suffix), rightOffset, dropRight(right, suffix))
      } else {
        // nothing in common - return the originals
        (leftOffset, left, rightOffset, right)
      }
    }

    // check for common prefix/suffix
    dropCommonSuffix(dropCommonPrefix(0, left, 0, right))
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
