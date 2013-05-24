package isabelle.eclipse.ui.editors

import isabelle.eclipse.core.text.DocumentModel
import isabelle.eclipse.ui.internal.IsabelleUIPlugin.{error, log}
import isabelle.eclipse.ui.util.SWTUtil

import org.eclipse.jface.text.{
  BadLocationException,
  DocumentEvent,
  IDocumentListener,
  ITextViewer,
  IViewportListener,
  JFaceTextUtil
}
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.events.{ControlAdapter, ControlEvent}


/** Tracker for changes in editor/document - upon change, updates the active perspective
  * in the Isabelle document model.
  * 
  * @author Andrius Velykis 
  */
trait DocumentPerspectiveTracker {

  /** A listener for scrolling events in the editor. Updates the active perspective upon scrolling. */
  lazy private val viewerViewportListener = new IViewportListener {
    override def viewportChanged(verticalOffset: Int) = updateActivePerspective()
  }
  
  /** A listener for resize events in the editor. Updates the active perspective upon editor resize. */
  lazy private val viewerControlListener = new ControlAdapter {
    override def controlResized(e: ControlEvent) = updateActivePerspective()
  }

  /** A listener for document events in the editor. Updates the active perspective upon document modification.
    * This is necessary because active perspective offsets change when editing.
    */
  lazy private val documentListener = new IDocumentListener {
    
      override def documentChanged(event: DocumentEvent) = updateActivePerspective()
      override def documentAboutToBeChanged(event: DocumentEvent) {}
    }
  
  protected def isabelleModel(): DocumentModel
  
  protected def textViewer(): Option[ITextViewer]

  private def textViewerControl: Option[StyledText] =
    textViewer flatMap (v => Option(v.getTextWidget))

  def initPerspective() {

    // listen to scroll and resize events
    // (text viewer must be available for init)
    textViewer.get.addViewportListener(viewerViewportListener)
    textViewer.get.getTextWidget.addControlListener(viewerControlListener)
    isabelleModel.document.addDocumentListener(documentListener)

    // update perspective with initial values
    updateActivePerspective()
  }
  
  def disposePerspective() {

    textViewer foreach (_.removeViewportListener(viewerViewportListener))
    // the widget may be null during disposal
    textViewerControl foreach (_.removeControlListener(viewerControlListener))
    isabelleModel.document.removeDocumentListener(documentListener)
  }

  /**
   * Updates the active perspective in the model. Finds the region currently
   * visible in the editor and marks that in the model as its perspective -
   * the area that should be submitted to the prover.
   */
  def updateActivePerspective() = SWTUtil.asyncUnlessDisposed(textViewerControl) {

    // only update if viewer is available
    textViewer foreach { v =>
      
      val (start, end) = visibleRange(v)
      isabelleModel.setActivePerspective(math.max(start, 0), math.max(end - start, 0))
    }
  }
  
  /** Calculates that start and end offsets of the currently visible text range */
  private def visibleRange(viewer: ITextViewer): (Int, Int) = {
    
    val visibleLines = JFaceTextUtil.getVisibleModelLines(viewer)

    if (visibleLines.getNumberOfLines > 0 && visibleLines.getStartLine >= 0) {
      // something is visible
      val document = isabelleModel.document

      try {
        
        val start = document.getLineOffset(visibleLines.getStartLine)
        val endLine = visibleLines.getStartLine + visibleLines.getNumberOfLines
        val end = if (endLine >= document.getNumberOfLines - 1) {
          document.getLength
        } else {
          document.getLineOffset(endLine) + document.getLineLength(endLine)
        }

        (start, math.max(start, end))
        
      } catch {
        
        case e: BadLocationException => {
          log(error(Some(e)))
          // something is visible, but problems calculating the perspective: use full document
          (0, document.getLength)
        }
      }
    } else {
      // no perspective
      (0, 0)
    }
  }
  
}
