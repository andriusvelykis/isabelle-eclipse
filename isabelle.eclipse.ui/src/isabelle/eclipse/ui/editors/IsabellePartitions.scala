package isabelle.eclipse.ui.editors

import org.eclipse.jface.text.rules.IPartitionTokenScanner
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner
import org.eclipse.jface.text.rules.IToken
import org.eclipse.jface.text.rules.Token
import org.eclipse.jface.text.rules.IPredicateRule
import org.eclipse.jface.text.rules.MultiLineRule
import org.eclipse.jface.text.rules.SingleLineRule
import org.eclipse.jface.text.rules.IWordDetector
import org.eclipse.jface.text.rules.WordRule
import org.eclipse.jface.text.rules.WordPatternRule

/* Definition of Isabelle partitioning and its partitions.
 * <p>
 * Also provides static factory methods to create partition scanners for theory/session modes.
 * </p>
 * 
 * @author Andrius Velykis
 */
object IsabellePartitions {

  /** The identifier of the Isabelle partitioning */
  val ISABELLE_PARTITIONING = "__isabelle_partitioning"
  
  val ISABELLE_COMMENT = "__isabelle_comment"
  val ISABELLE_VERBATIM = "__isabelle_verbatim"
  val ISABELLE_STRING = "__isabelle_string"
  val ISABELLE_ALTSTRING = "__isabelle_altstring"
  val ISABELLE_KEYWORD = "__isabelle_keyword"

  /** All valid Isabelle partition types */
  def contentTypes = Array(
      ISABELLE_COMMENT, ISABELLE_VERBATIM, ISABELLE_STRING, ISABELLE_ALTSTRING, ISABELLE_KEYWORD)
    
  private val THEORY_KEYWORDS = List("header", "theory", "imports", "uses", "begin", "end")
  private val SESSION_KEYWORDS = List("session", "parent", "imports", "uses", "options", "dependencies")

  private val wordDetector = new IWordDetector {

    val noWordSep = List('_', '\'', '.', '?')

    def isWordStart(c: Char) = isIsabelleChar(c)
    def isWordPart(c: Char) = isIsabelleChar(c)
    def isIsabelleChar(c: Char) = 
      Character.isLetter(c) || Character.isDigit(c) || (noWordSep contains c)
  }

  /** Creates a partition scanner for Isabelle Theory mode, which returns tokens with partition IDs */
  def createTheoryScanner(): IPartitionTokenScanner = {

    val rules = textRules// ++ keywordRules(THEORY_KEYWORDS)

    val scanner = new RuleBasedPartitionScanner()
    scanner.setPredicateRules(rules.toArray)
    scanner
  }
  
  /** Creates a partition scanner for Isabelle Session mode, which returns tokens with partition IDs */
  def createSessionScanner(): IPartitionTokenScanner = {

    val rules = textRules// ++ keywordRules(SESSION_KEYWORDS)

    val scanner = new RuleBasedPartitionScanner()
    scanner.setPredicateRules(rules.toArray)
    scanner
  }
  
  /** Create text rules (e.g. comments) */
  private def textRules = List(
      new MultiLineRule("(*", "*)", new Token(ISABELLE_COMMENT), '\\'),
      new MultiLineRule("{*", "*}", new Token(ISABELLE_VERBATIM)),
      new MultiLineRule("`", "`", new Token(ISABELLE_ALTSTRING), '\\'),
      new MultiLineRule("\"", "\"", new Token(ISABELLE_STRING), '\\'))
  
// keyword rules not working at the moment
// 
//  /** Create rules to scan basic top level keywords */
//  private def keywordRules(keywords: List[String]) = {
//    val keyword = new Token(ISABELLE_KEYWORD)
//    
//    THEORY_KEYWORDS map (word => new WordPatternRule(wordDetector, word, word, keyword))
//  }
  
}