package isabelle.eclipse.ui.editors

import org.eclipse.jface.text.{BadLocationException, IDocument, ITextSelection, ITextViewer}
import org.eclipse.jface.viewers.ISelectionProvider
import org.eclipse.ui.IEditorPart


/**
 * Editor utilities (separate from EditorUtil because that one is in Java at the moment)
 * 
 * @author Andrius Velykis
 */
object EditorUtil2 {


  def replaceSelected(editor: IEditorPart, text: String): Unit =
    Option(EditorUtil.getTextViewer(editor)) foreach (replaceSelected(_, text))


  def replaceSelected(viewer: ITextViewer, text: String): Unit = withSelection(viewer) {
    case (offset, length) => {
      // set the text
      viewer.getDocument.replace(offset, length, text)
      // advance the cursor to after the text
      viewer.getTextWidget.setCaretOffset(offset + text.length)
    }
  }


  def insertAsNewLine(viewer: ITextViewer, text: String) = withSelection(viewer) {
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


  private def safeLineInfoAt(document: IDocument, offset: Int): Option[(Int, Int)] = {
    try {
      val line = document.getLineInformationOfOffset(offset)
      Some(line.getOffset, line.getOffset + line.getLength)
    } catch {
      case _: BadLocationException => None
    }
  }


  def withSelection[R](viewer: ITextViewer)(f: (Int, Int) => R): R = {

    val selection = textSelection(viewer.getSelectionProvider)

    // get offset from selection or the caret
    val offset = selection map (_.getOffset) getOrElse viewer.getTextWidget.getCaretOffset
    val length = selection map (_.getLength) getOrElse 0

    f(offset, length)
  }


  def textSelection(selectionProvider: ISelectionProvider): Option[ITextSelection] = {
    val selection = selectionProvider.getSelection

    if (selection.isEmpty) {
      None
    } else selection match {
      case textSelection: ITextSelection => Some(textSelection)
      case _ => None
    }
  }


  /**
   * Executes given function while preserving scroll position in the given text viewer.
   */
  def preserveScroll(viewer: ITextViewer)(f: => Unit) {
    val topIndex = viewer.getTopIndex
    f
    if (viewer.getTopIndex != topIndex) {
      viewer.setTopIndex(topIndex)
    }
  }
  
}
