package isabelle.eclipse.ui.editors

import org.eclipse.jface.text.{BadLocationException, IDocument}
import org.eclipse.jface.text.contentassist.{
  ICompletionProposal,
  ICompletionProposalExtension6,
  IContextInformation
}
import org.eclipse.jface.viewers.StyledString
import org.eclipse.swt.graphics.{Image, Point}


/**
 * A completion proposal that supports styled string as the display text.
 * 
 * Adapted from `org.eclipse.jface.text.contentassist.CompletionProposal`.
 * 
 * @author Andrius Velykis
 */
class StyledCompletionProposal(replacementString: String,
                               replacementOffset: Int,
                               replacementLength: Int,
                               cursorPosition: Int,
                               image: Option[Image],
                               displayString: StyledString,
                               contextInformation: Option[IContextInformation],
                               additionalProposalInfo: Option[String])
    extends ICompletionProposal with ICompletionProposalExtension6 {

  override def apply(document: IDocument) {
    try {
      document.replace(replacementOffset, replacementLength, replacementString)
    } catch {
      case x: BadLocationException => // ignore
    }
  }

  override def getSelection(document: IDocument): Point = {
    return new Point(replacementOffset + cursorPosition, 0);
  }

  override def getContextInformation: IContextInformation = contextInformation.orNull

  override def getImage: Image = image.orNull

  override def getDisplayString: String = displayString.getString

  override def getAdditionalProposalInfo: String = additionalProposalInfo.orNull

  override def getStyledDisplayString: StyledString = displayString

}
