package isabelle.eclipse.ui.preferences

import isabelle.eclipse.ui.editors.IsabellePartitions
import isabelle.Outer_Syntax

/** Definitions of Isabelle syntax classes and translations from 
  * Isabelle markup/tokens/etc (objects below).
  * 
  * @author Andrius Velykis
  */
object IsabelleSyntaxClasses {

  val DEFAULT = IsabelleSyntaxClass("Others", "syntax.default")
  val UNDEFINED = IsabelleSyntaxClass("Undefined", "syntax.undef")
  
  val COMMENT = IsabelleSyntaxClass("Comments", "syntax.comment")
  val INNER_COMMENT = IsabelleSyntaxClass("Inner Comment", "syntax.innerComment")
  val VERBATIM = IsabelleSyntaxClass("Verbatim", "syntax.verbatim")
  val STRING = IsabelleSyntaxClass("Strings", "syntax.string")
  val INNER_STRING = IsabelleSyntaxClass("Inner Strings", "syntax.innerString")
  val KEYWORD = IsabelleSyntaxClass("Keywords", "syntax.keyword")
  val OPERATOR = IsabelleSyntaxClass("Operators", "syntax.op")
  val LITERAL = IsabelleSyntaxClass("Literals", "syntax.literal")
  val DELIMITER = IsabelleSyntaxClass("Delimiters", "syntax.delimiter")
  val TYPE = IsabelleSyntaxClass("Type Variables", "syntax.typevar")
  val FREE = IsabelleSyntaxClass("Free Variables", "syntax.free")
  val SKOLEM = IsabelleSyntaxClass("Skolem Variables", "syntax.skolem")
  val BOUND = IsabelleSyntaxClass("Bound Variables", "syntax.bound")
  val VAR = IsabelleSyntaxClass("Variables", "syntax.var")
  val DYN_FACT = IsabelleSyntaxClass("Dynamic Facts", "syntax.dynFact")
  val ANTIQ = IsabelleSyntaxClass("Antiquotations", "syntax.antiq")
  val CLASS = IsabelleSyntaxClass("Class Entities", "syntax.entity.class")
  
  val CMD = IsabelleSyntaxClass("Proof Commands", "syntax.cmd.cmd")
  val CMD_SCRIPT = IsabelleSyntaxClass("Proof Script Commands", "syntax.cmd.script")
  val CMD_GOAL = IsabelleSyntaxClass("Proof Goal Commands", "syntax.cmd.goal")
  
  val ML_KEYWORD = IsabelleSyntaxClass("ML Keywords", "syntax.ml.keyword")
  val ML_NUMERAL = IsabelleSyntaxClass("ML Numerals", "syntax.ml.num")
  val ML_STRING = IsabelleSyntaxClass("ML Strings", "syntax.ml.string")
  val ML_COMMENT = IsabelleSyntaxClass("ML Comments", "syntax.ml.comment")
  val ML_BAD = IsabelleSyntaxClass("ML Malformed Text", "syntax.ml.bad")
  
  val ALL_SYNTAX_CLASSES = List(DEFAULT, COMMENT, INNER_COMMENT, VERBATIM, STRING, INNER_STRING, KEYWORD,
      LITERAL, DELIMITER, TYPE, FREE, SKOLEM, BOUND, VAR, DYN_FACT, ANTIQ, 
      ML_KEYWORD, ML_NUMERAL, ML_STRING, ML_COMMENT, ML_BAD)

  val COLOUR_SUFFIX = ".colour"
  val BOLD_SUFFIX = ".bold"
  val ITALIC_SUFFIX = ".italic"
  val STRIKETHROUGH_SUFFIX = ".strikethrough"
  val UNDERLINE_SUFFIX = ".underline"
  
}

object IsabellePartitionToSyntaxClass {

  import IsabellePartitions._

  def apply(partition: String): IsabelleSyntaxClass = partition match {
    case ISABELLE_COMMENT => IsabelleSyntaxClasses.COMMENT
    case ISABELLE_VERBATIM => IsabelleSyntaxClasses.VERBATIM
    case ISABELLE_STRING | ISABELLE_ALTSTRING => IsabelleSyntaxClasses.STRING
    case ISABELLE_KEYWORD => IsabelleSyntaxClasses.KEYWORD
    case _ => IsabelleSyntaxClasses.UNDEFINED
  }
}

object IsabelleTokenToSyntaxClass {

  import isabelle.Token.Kind._
  import isabelle.Keyword._

  def apply(syntax: Outer_Syntax, token: isabelle.Token): IsabelleSyntaxClass =
    if (token.is_command)
      syntax.keyword_kind(token.content).getOrElse("") match {
//        case THY_END => IsabelleSyntaxClasses.CLASS
        case THY_SCRIPT | PRF_SCRIPT => IsabelleSyntaxClasses.CMD_SCRIPT
        case PRF_ASM | PRF_ASM_GOAL => IsabelleSyntaxClasses.CMD_GOAL
        case _ => IsabelleSyntaxClasses.CMD
      }
    else if (token.is_operator) IsabelleSyntaxClasses.OPERATOR
    else token.kind match {
      case COMMAND | KEYWORD => IsabelleSyntaxClasses.KEYWORD
//      case IDENT | LONG_IDENT | SYM_IDENT | VAR => IsabelleSyntaxClasses.VAR
//      case TYPE_IDENT | TYPE_VAR => IsabelleSyntaxClasses.TYPE
      case STRING | ALT_STRING => IsabelleSyntaxClasses.STRING
      case VERBATIM => IsabelleSyntaxClasses.VERBATIM
      case COMMENT => IsabelleSyntaxClasses.COMMENT
      case NAT | FLOAT | SPACE | UNPARSED | _ => IsabelleSyntaxClasses.UNDEFINED
    }
}

object IsabelleMarkupToSyntaxClass {

  import isabelle.Markup. _

  def apply(markupType: String): IsabelleSyntaxClass = markupType match {
    case STRING | ALTSTRING => IsabelleSyntaxClasses.STRING
    case VERBATIM => IsabelleSyntaxClasses.VERBATIM
    case LITERAL => IsabelleSyntaxClasses.LITERAL
    case DELIMITER => IsabelleSyntaxClasses.DELIMITER
    case TFREE | TVAR => IsabelleSyntaxClasses.TYPE
    case FREE => IsabelleSyntaxClasses.FREE
    case SKOLEM => IsabelleSyntaxClasses.SKOLEM
    case BOUND => IsabelleSyntaxClasses.BOUND
    case VAR => IsabelleSyntaxClasses.VAR
    case INNER_STRING => IsabelleSyntaxClasses.INNER_STRING
    case INNER_COMMENT => IsabelleSyntaxClasses.INNER_COMMENT
    case DYNAMIC_FACT => IsabelleSyntaxClasses.DYN_FACT
    case ANTIQ => IsabelleSyntaxClasses.ANTIQ
    case ML_KEYWORD => IsabelleSyntaxClasses.ML_KEYWORD
    case ML_DELIMITER => IsabelleSyntaxClasses.DELIMITER
    case ML_NUMERAL => IsabelleSyntaxClasses.ML_NUMERAL
    case ML_CHAR | ML_STRING => IsabelleSyntaxClasses.ML_STRING
    case ML_COMMENT => IsabelleSyntaxClasses.ML_COMMENT
    case ML_MALFORMED => IsabelleSyntaxClasses.ML_BAD
    case _ => IsabelleSyntaxClasses.UNDEFINED
  }
}

