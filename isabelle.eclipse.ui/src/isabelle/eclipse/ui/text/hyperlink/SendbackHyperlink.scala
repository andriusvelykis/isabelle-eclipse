package isabelle.eclipse.ui.text.hyperlink

import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.ITextViewer
import org.eclipse.jface.text.hyperlink.IHyperlink

import isabelle.Document.Snapshot
import isabelle.Document_ID
import isabelle.Markup
import isabelle.Position
import isabelle.Properties
import isabelle.eclipse.core.text.DocumentModel
import isabelle.eclipse.ui.editors.EditorUtil2.insertAsNewLine
import isabelle.eclipse.ui.editors.EditorUtil2.replaceSelected
import isabelle.eclipse.ui.internal.IsabelleUIPlugin.error
import isabelle.eclipse.ui.internal.IsabelleUIPlugin.log


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

        case (_, Some(viewer)) => if (sendbackProps.exists(_ == Markup.PADDING_LINE)) {
          insertAsNewLine(viewer, sendbackText)
        } else {
          replaceSelected(viewer, sendbackText)
        }

        case _ => log(error(msg = Some("Invalid sendback link target - target viewer undefined")))
      }
    }
  }


  private def tryReplaceCommand(snapshot: Snapshot,
                                document: IDocument,
                                execId: Document_ID.Exec,
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

}
