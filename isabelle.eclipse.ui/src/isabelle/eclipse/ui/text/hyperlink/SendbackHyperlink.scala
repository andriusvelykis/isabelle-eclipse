package isabelle.eclipse.ui.text.hyperlink

import org.eclipse.jface.text.{BadLocationException, IDocument, IRegion, ITextSelection, ITextViewer}
import org.eclipse.jface.text.hyperlink.IHyperlink

import isabelle.{Markup, Position, Properties}
import isabelle.Document
import isabelle.Document.Snapshot
import isabelle.eclipse.core.text.DocumentModel
import isabelle.eclipse.ui.internal.IsabelleUIPlugin.{error, log}


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

        case _ => log(error(msg = Some("Invalid sendback link target - target viewer undefined")))
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
      val (replaceOffset, paddedText) = if (length != 0) {
        (offset, text)
      } else safeLineInfoAt(document, offset) match {
        case None => (offset, text)

        // if nothing is selected, try to work out how to add text as new line
        case Some((lineOffset, lineEnd)) => 
          if (lineOffset == offset) {
            // cursor at the start of the line, append newline
            (offset, text + "\n")
          } else {
            // cursor is either at the end or in the middle of the line:
            // replace at the end of the line with leading newline
            (lineEnd, "\n" + text)
          }
      }

      // set the text
      document.replace(replaceOffset, length, paddedText)
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


  private def safeLineInfoAt(document: IDocument, offset: Int): Option[(Int, Int)] = {
    try {
      val line = document.getLineInformationOfOffset(offset)
      Some(line.getOffset, line.getOffset + line.getLength)
    } catch {
      case _: BadLocationException => None
    }
  }

}
