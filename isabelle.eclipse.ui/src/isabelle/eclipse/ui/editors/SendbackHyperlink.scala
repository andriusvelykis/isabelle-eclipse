package isabelle.eclipse.ui.editors

import org.eclipse.jface.text.{BadLocationException, IDocument, IRegion, ITextSelection, ITextViewer}
import org.eclipse.jface.text.hyperlink.IHyperlink

import isabelle.{Markup, Position, Properties}
import isabelle.Document
import isabelle.Document.Snapshot
import isabelle.eclipse.core.text.DocumentModel
import isabelle.eclipse.ui.IsabelleUIPlugin


/**
 * A hyperlink that replaces text editor contents with given text.
 * 
 * Used for "sendback" operation, when prover suggests text to replace a command.
 * 
 * @author Andrius Velykis
 */
class SendbackHyperlink(linkRegion: IRegion,
                        targetDocModel: => Option[DocumentModel],
                        targetViewer: => Option[ITextViewer],
                        sendbackProps: Properties.T,
                        sendbackText: String,
                        targetName: Option[String] = Some("Replace command in editor"))
    extends IHyperlink {

  override def getHyperlinkRegion(): IRegion = linkRegion

  override def getTypeLabel = "Replace in Editor"

  override def getHyperlinkText(): String = targetName.orNull

  override def open() = targetDocModel foreach { model =>
    val snapshot = model.snapshot

    if (!snapshot.is_outdated) {

      (sendbackProps, targetViewer) match {

        case (Position.Id(execId), _) =>
          tryReplaceCommand(snapshot, model.document, execId, sendbackText)

        // TODO try out non-position sendback (not tested yet)
        case (_, Some(viewer)) => if (sendbackProps.exists(_ == Markup.PADDING_LINE)) {
          insertLinePadding(viewer, sendbackText)
        } else {
          replaceSelected(viewer, sendbackText)
        }

        case _ => IsabelleUIPlugin.log(
            "Invalid sendback link target - target viewer undefined", null)
      }

    }
  }


  private def tryReplaceCommand(snapshot: Snapshot,
                                document: IDocument,
                                execId: Document.ID,
                                text: String) {

    snapshot.state.execs.get(execId).map(_.command) match {
      case Some(command) =>
        snapshot.node.command_start(command) match {
          case Some(start) =>
            // replace the command text in the document with sendback text
            document.replace(start, command.proper_range.length, text)
          case None =>
        }
      case None =>
    }
  }


  /* structured insert */

  private def insertLinePadding(viewer: ITextViewer, text: String) = withSelection(viewer) {
    case (offset, length) => {

      val document = viewer.getDocument
      val paddedText = if (length == 0) {
        text
      } else {
        // if nothing is selected, add padding to text if needed
        def pad(offset: Int): String =
          if (safeTextAt(document, offset) == Some("\n")) "" else "\n"

        pad(offset - 1) + text + pad(offset)
      }

      // set the text
      document.replace(offset, length, paddedText)
    }
  }


  private def replaceSelected(viewer: ITextViewer, text: String) = withSelection(viewer) {
    case (offset, length) => {
      // set the text
      viewer.getDocument.replace(offset, length, text)
    }
  }


  private def withSelection[R](viewer: ITextViewer)(f: (Int, Int) => R) = {

    val selection = textViewerSelection(viewer)
    val document = viewer.getDocument

    // get offset from selection or the caret
    val offset = selection map (_.getOffset) getOrElse viewer.getTextWidget.getCaretOffset
    val length = selection map (_.getLength) getOrElse 0

    f(offset, length)
  }


  private def textViewerSelection(viewer: ITextViewer): Option[ITextSelection] = {
    val selection = viewer.getSelectionProvider.getSelection

    if (selection.isEmpty) {
      None
    } else selection match {
      case textSelection: ITextSelection => Some(textSelection)
      case _ => None
    }
  }


  private def safeTextAt(document: IDocument, offset: Int): Option[String] =
    try {
      Some(String.valueOf(document.getChar(offset)))
    } catch {
      case _: BadLocationException => None
    }

}
