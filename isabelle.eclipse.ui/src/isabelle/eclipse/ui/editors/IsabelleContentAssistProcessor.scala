package isabelle.eclipse.ui.editors

import scala.util.{Either, Failure, Success, Try}

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

import isabelle.{Outer_Syntax, Symbol}
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

  override def computeCompletionProposals(viewer: ITextViewer,
                                          offset: Int): Array[ICompletionProposal] = {

    val testCompletions = syntax match {
      case None => Left("Isabelle syntax not available.")
      case Some(syntax) => complete(syntax, viewer, offset)
    }

    // record the error if available
    lastError = testCompletions.left.toOption

    // return null if no completions are available
    val resultsOpt = testCompletions.right.toOption
    val resultsArrayOpt = resultsOpt.filterNot(_.isEmpty).map(_.toArray)
    resultsArrayOpt.orNull
  }

  /**
   * Automatically activate completion proposals for \ or <
   * (e.g. for when typing symbols such as \<forall>)
   */
  override def getCompletionProposalAutoActivationCharacters: Array[Char] =
    Array('\\', '<')


  override def getErrorMessage: String = lastError.orNull


  private def complete(syntax: Outer_Syntax,
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
          Right(List())
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


  private def calculateCompletions(syntax: Outer_Syntax, text: String): List[CompletionInfo] =
    syntax.completion.complete(text) match {

      case None => Nil

      // TODO sort?
      case Some((word, cs)) => cs map { raw => CompletionInfo(word, Symbol.decode(raw), raw) }
    }


  private def createProposalItem(image: Image, offset: Int)(
                                   info: CompletionInfo): ICompletionProposal = {

    val word = info.word
    val replaceOffset = offset - word.length

    val displayStr = completionDisplay(info)

    val replaceStr = info.decoded

    new CompletionProposal(replaceStr, replaceOffset, word.length, replaceStr.length,
                           image, displayStr, null, null)
  }


  private def completionDisplay(info: CompletionInfo): String = {
    if (info.decoded == info.raw) {
      info.decoded
    } else {
      
      info.decoded + " : " + info.raw
    }
  }


  /* Not supporting context information at the moment: */

  override def computeContextInformation(viewer: ITextViewer,
                                         offset: Int): Array[IContextInformation] = null

  override def getContextInformationAutoActivationCharacters: Array[Char] = null

  override def getContextInformationValidator: IContextInformationValidator = null


  private case class CompletionInfo(word: String, decoded: String, raw: String)

}
