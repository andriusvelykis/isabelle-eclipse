package isabelle.eclipse.ui.preferences

import isabelle.Outer_Syntax
import isabelle.eclipse.ui.editors.IsabellePartitions


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
  val KEYWORD2 = IsabelleSyntaxClass("Keywords 2", "syntax.keyword2")
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
  
  val ACTIVE = IsabelleSyntaxClass("Isabelle Action Links", "syntax.active")
  val DIALOG_SELECTED = IsabelleSyntaxClass("Isabelle Selected Dialog", "syntax.dialog.selected")
  
  val ALL_SYNTAX_CLASSES = List(DEFAULT, COMMENT, INNER_COMMENT, VERBATIM, STRING, INNER_STRING,
      KEYWORD, KEYWORD2,
      LITERAL, DELIMITER, TYPE, FREE, SKOLEM, BOUND, VAR, DYN_FACT, ANTIQ, 
      ML_KEYWORD, ML_NUMERAL, ML_STRING, ML_COMMENT,
      ACTIVE)

  val COLOR_SUFFIX = ".color"
  val COLOR_ENABLED_SUFFIX = ".colorEnabled"
  val BACKGROUND_COLOR_SUFFIX = ".bgColor"
  val BACKGROUND_COLOR_ENABLED_SUFFIX = ".bgColorEnabled"
  val BOLD_SUFFIX = ".bold"
  val ITALIC_SUFFIX = ".italic"
  val STRIKETHROUGH_SUFFIX = ".strikethrough"
  val UNDERLINE_SUFFIX = ".underline"
  val UNDERLINE_STYLE_SUFFIX = ".underlineStyle"
  
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

  val commandClasses: Map[String, IsabelleSyntaxClass] = Map(
    THY_END -> IsabelleSyntaxClasses.KEYWORD2,
    THY_SCRIPT -> IsabelleSyntaxClasses.CMD_SCRIPT,
    PRF_SCRIPT -> IsabelleSyntaxClasses.CMD_SCRIPT,
    PRF_ASM -> IsabelleSyntaxClasses.CMD_GOAL,
    PRF_ASM_GOAL -> IsabelleSyntaxClasses.CMD_GOAL
  ).withDefaultValue(IsabelleSyntaxClasses.CMD)

  val tokenClasses: Map[Value, IsabelleSyntaxClass] = Map(
    KEYWORD -> IsabelleSyntaxClasses.KEYWORD2,
//    IDENT -> IsabelleSyntaxClasses.VAR,
//    LONG_IDENT -> IsabelleSyntaxClasses.VAR,
//    SYM_IDENT -> IsabelleSyntaxClasses.VAR,
//    VAR -> IsabelleSyntaxClasses.VAR,
//    TYPE_IDENT -> IsabelleSyntaxClasses.TYPE,
//    TYPE_VAR -> IsabelleSyntaxClasses.TYPE,
    NAT -> IsabelleSyntaxClasses.UNDEFINED,
    FLOAT -> IsabelleSyntaxClasses.UNDEFINED,
    STRING -> IsabelleSyntaxClasses.STRING,
    ALT_STRING -> IsabelleSyntaxClasses.STRING,
    VERBATIM -> IsabelleSyntaxClasses.VERBATIM,
    SPACE -> IsabelleSyntaxClasses.UNDEFINED,
    COMMENT -> IsabelleSyntaxClasses.COMMENT//,
//    ERROR -> IsabelleSyntaxClasses.UNDEFINED
  ).withDefaultValue(IsabelleSyntaxClasses.UNDEFINED)

  def apply(syntax: Outer_Syntax, token: isabelle.Token): IsabelleSyntaxClass =
    if (token.is_command) commandClasses(syntax.keyword_kind(token.content).getOrElse(""))
    else if (token.is_operator) IsabelleSyntaxClasses.OPERATOR
    else tokenClasses(token.kind)
}

object IsabelleMarkupToSyntaxClass {
  
  val DIALOG_SELECTED = "dialog-selected"

  import isabelle.Markup._

  val markupClasses: Map[String, IsabelleSyntaxClass] = Map(
    KEYWORD1 -> IsabelleSyntaxClasses.KEYWORD,
    KEYWORD2 -> IsabelleSyntaxClasses.KEYWORD2,
    STRING -> IsabelleSyntaxClasses.STRING,
    ALTSTRING -> IsabelleSyntaxClasses.STRING,
    VERBATIM -> IsabelleSyntaxClasses.VERBATIM,
    LITERAL -> IsabelleSyntaxClasses.LITERAL,
    DELIMITER -> IsabelleSyntaxClasses.DELIMITER,
    TFREE -> IsabelleSyntaxClasses.TYPE,
    TVAR -> IsabelleSyntaxClasses.TYPE,
    FREE -> IsabelleSyntaxClasses.FREE,
    SKOLEM -> IsabelleSyntaxClasses.SKOLEM,
    BOUND -> IsabelleSyntaxClasses.BOUND,
    VAR -> IsabelleSyntaxClasses.VAR,
    INNER_STRING -> IsabelleSyntaxClasses.INNER_STRING,
    INNER_COMMENT -> IsabelleSyntaxClasses.INNER_COMMENT,
    DYNAMIC_FACT -> IsabelleSyntaxClasses.DYN_FACT,
    ANTIQ -> IsabelleSyntaxClasses.ANTIQ,
    ML_KEYWORD -> IsabelleSyntaxClasses.ML_KEYWORD,
    ML_DELIMITER -> IsabelleSyntaxClasses.DELIMITER,
    ML_NUMERAL -> IsabelleSyntaxClasses.ML_NUMERAL,
    ML_CHAR -> IsabelleSyntaxClasses.ML_STRING,
    ML_STRING -> IsabelleSyntaxClasses.ML_STRING,
    ML_COMMENT -> IsabelleSyntaxClasses.ML_COMMENT
    ).withDefaultValue(IsabelleSyntaxClasses.UNDEFINED)

  val extendedMarkupClasses: Map[String, IsabelleSyntaxClass] = markupClasses ++ Map(
    BROWSER -> IsabelleSyntaxClasses.ACTIVE,
    GRAPHVIEW -> IsabelleSyntaxClasses.ACTIVE,
    SENDBACK -> IsabelleSyntaxClasses.ACTIVE,
    DIALOG -> IsabelleSyntaxClasses.ACTIVE,
    DIALOG_SELECTED -> IsabelleSyntaxClasses.DIALOG_SELECTED
    )
  
  val markups = markupClasses.keySet

  def apply(markupType: String): IsabelleSyntaxClass = extendedMarkupClasses(markupType)
}

