package isabelle.eclipse.ui.editors

import scala.util.{Either, Failure, Success, Try}
import scala.util.parsing.combinator.RegexParsers

import org.eclipse.jface.resource.ResourceManager
import org.eclipse.jface.text.{IDocument, ITextViewer}
import org.eclipse.jface.text.contentassist.{
  CompletionProposal,
  ICompletionProposal,
  IContentAssistProcessor,
  IContextInformation,
  IContextInformationValidator
}
import org.eclipse.swt.graphics.Image

import isabelle.{Outer_Syntax, Scan, Symbol}
import isabelle.eclipse.core.IsabelleCore
import isabelle.eclipse.ui.internal.IsabelleImages
import isabelle.eclipse.ui.internal.IsabelleUIPlugin.{error, log}


/**
 * Provides content assist proposals for Isabelle text.
 *
 * @author Andrius Velykis
 */
class IsabelleContentAssistProcessor(syntax: => Option[Outer_Syntax],
                                     resourceManager: ResourceManager)
    extends IContentAssistProcessor {

  private var lastError: Option[String] = None

  // Lazily load symbol abbreviations lexicon, map and max length.
  // This assumes that they are loaded after system symbols are initialised.
  // 
  // Note that abbreviations do not change after initialisation, so it is ok to store them.
  // We cannot use these from Outer_Syntax.completion, because the abbreviations there are
  // backwards - we need forward ones.
  private lazy val abbrevsMap =
    for {
      (sym, abbrev) <- Symbol.abbrevs if !IsabelleContentAssistProcessor.is_word(abbrev)
    } yield {
      (abbrev, sym)
    }

  private lazy val abbrevsLex = Scan.Lexicon.empty ++ abbrevsMap.keys
  private lazy val abbrevsMaxLength =
    if (abbrevsMap.isEmpty) 0
    else abbrevsMap.keys.maxBy(_.length).length

  // for activation characters, use `\` plus the first characters of abbreviations
  private val activationSlash = '\\'
  private lazy val activationChars =
    activationSlash :: abbrevsMap.keySet.map(_.charAt(0)).toList


  private def symbolsInit = IsabelleCore.isabelle.isInit


  override def computeCompletionProposals(viewer: ITextViewer,
                                          offset: Int): Array[ICompletionProposal] = {

    val testCompletions = syntax match {
      case None => Left("Isabelle syntax not available.")
      case Some(syntax) => completions(syntax, viewer, offset)
    }

    // record the error if available
    lastError = testCompletions.left.toOption

    // return null if no completions are available
    val resultsOpt = testCompletions.right.toOption
    val resultsArrayOpt = resultsOpt.filterNot(_.isEmpty).map(_.toArray)
    resultsArrayOpt.orNull
  }

  /**
   * Automatically activate completion proposals for `\` and start symbols of all abbreviations
   * (e.g. for when typing symbols such as `\<forall>`)
   */
  override def getCompletionProposalAutoActivationCharacters: Array[Char] =
    if (symbolsInit) activationChars.toArray
    else Array(activationSlash)


  override def getErrorMessage: String = lastError.orNull


  private def completions(syntax: Outer_Syntax,
                          viewer: ITextViewer,
                          offset: Int): Either[String, List[ICompletionProposal]] = {

    lineTextAt(viewer.getDocument, offset) match {

      case Failure(ex) => {
        // failure - log the exception and report error
        log(error(Some(ex)))
        Left(ex.getMessage)
      }

      case Success(lineText) => {
        val completions = calculateCompletions(syntax, lineText)

        if (completions.isEmpty) {
          Right(Nil)
        } else {

          val image = resourceManager.createImageWithDefault(IsabelleImages.CONTENT_ASSIST)
          val createItem = createProposalItem(image, offset) _

          val completionItems = completions map createItem
          Right(completionItems)
        }
      }
    }
  }


  private def lineTextAt(document: IDocument, offset: Int) = Try {
    val line = document.getLineOfOffset(offset)
    val start = document.getLineOffset(line)
    val text = document.get(start, offset - start)
    text
  }


  /**
   * Calculates Isabelle completions for the given text.
   * 
   * Checks only the last word for completions.
   * Provides lookahead completions, e.g. when only part of abbreviation/word is entered.
   * Combines completions from abbreviations and words.
   * Returns the longest-matching completions only (to avoid sub-completions).
   */
  private def calculateCompletions(syntax: Outer_Syntax, text: String): List[CompletionInfo] = {
    val lastWord = text.reverseIterator.takeWhile(c => !c.isWhitespace).mkString.reverse
    if (lastWord.isEmpty) Nil
    else {
      val abbrevCs = abbrevCompletions(lastWord).sortBy(abbrevSort)
      val wordCs = wordCompletions(syntax, lastWord).sortBy(_.word.toLowerCase)

      if (abbrevCs.isEmpty && wordCs.isEmpty) {
        Nil
      } else {
        val allCs = abbrevCs ::: wordCs
  
        // only take the ones with the maximum match length
        // this is necessary to avoid sub-matching in the long string
        val maxMatchLength = allCs.maxBy(_.matched.length).matched.length
        val matchCs = allCs.filter (_.matched.length == maxMatchLength)
  
        matchCs
      }
    }
  }


  /**
   * Calculates abbreviation completions.
   * 
   * Checks maching abbreviations by starting from the longest possible match and then goes to
   * check the shorter ones. This allows matching the longest,
   * e.g. `<-` would match `<->`, but not `->`.
   */
  private def abbrevCompletions(text: String): List[CompletionInfo] = {

    val length = text.length
    val testLength = abbrevsMaxLength min length
    // take the max abbreviation length from the end
    val testText = text.substring(length - testLength, length)

    val shorteningText = testText #:: (1 until testLength).toStream.map (testText.substring(_))

    val cs = shorteningText.map (abbrevsLex.completions)
    val textCompletions = shorteningText zip cs

    val nonEmptyCompletions = textCompletions.filterNot (_._2.isEmpty)

    nonEmptyCompletions.headOption match {
      // no abbreviation completions
      case None => Nil

      case Some((text, cs)) => {
        cs.map (c => CompletionInfo(text, c, abbrevsMap(c)))
      }
    }
  }


  /**
   * Order abbreviations by their abbreviation length, then by lowercase alphabetical,
   * but moving control symbols to the end.
   * 
   * This should give the "best matching" abbreviations (shortest length) first.
   */
  private def abbrevSort(completion: CompletionInfo) = {
    val control = completion.raw.startsWith("\\<^")
    (completion.word.length, control, completion.raw.toLowerCase)
  }


  private def wordCompletions(syntax: Outer_Syntax, text: String): List[CompletionInfo] = {
    text match {
      case IsabelleContentAssistProcessor.WordStart(matched) => {
        val wordsLex = syntax.completion.words_lex
        val wordsMap = syntax.completion.words_map
        val words = wordsLex.completions(matched)

        words map (w => CompletionInfo(matched, w, wordsMap(w)))
      }

      case _ => Nil
    }
  }


  private def createProposalItem(image: Image, offset: Int)(
                                   info: CompletionInfo): ICompletionProposal = {

    val matched = info.matched
    val replaceOffset = offset - matched.length

    val displayStr = completionDisplay(info)

    val replaceStr = info.decoded

    new CompletionProposal(replaceStr, replaceOffset, matched.length, replaceStr.length,
                           image, displayStr, null, null)
  }


  private def completionDisplay(info: CompletionInfo): String = {
    val strs = List(info.word, info.decoded, info.raw).distinct

    strs mkString " : "
  }


  /* Not supporting context information at the moment: */

  override def computeContextInformation(viewer: ITextViewer,
                                         offset: Int): Array[IContextInformation] = null

  override def getContextInformationAutoActivationCharacters: Array[Char] = null

  override def getContextInformationValidator: IContextInformationValidator = null

  private case class CompletionInfo(matched: String, word: String, raw: String) {
    lazy val decoded = Symbol.decode(raw)
  }

}

object IsabelleContentAssistProcessor {

  private object WordStart {
    def unapply(text: String): Option[String] =
      // special case for just the '\' symbol
      if (text.endsWith("\\")) Some("\\")
      else Parse.read(text)
  }

  /* word completion, adapted from isabelle.Completion */

  private val word_regex = "[a-zA-Z0-9_']+".r
  private def is_word(s: CharSequence): Boolean = word_regex.pattern.matcher(s).matches
  
  private object Parse extends RegexParsers {
    override val whiteSpace = "".r

    def reverse_symbol: Parser[String] = """>[A-Za-z0-9_']+\^?<\\""".r
    // allow 0-length text here, e.g. "\<"
    def reverse_symb: Parser[String] = """[A-Za-z0-9_']*\^?<\\""".r
    // allow 1-length word
    def word: Parser[String] = "[a-zA-Z0-9_']+".r

    def read(in: String): Option[String] = {
      val reverse_in = in.reverse
      parse((reverse_symbol | reverse_symb | word) ^^ (_.reverse), reverse_in) match {
        case Success(result, _) => Some(result)
        case _ => None
      }
    }
  }

}
