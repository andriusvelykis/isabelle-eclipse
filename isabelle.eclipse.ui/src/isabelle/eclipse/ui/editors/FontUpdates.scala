package isabelle.eclipse.ui.editors

import org.eclipse.jface.resource.JFaceResources
import org.eclipse.jface.text.ITextViewerExtension
import org.eclipse.jface.text.source.ISourceViewer
import org.eclipse.jface.util.{IPropertyChangeListener, PropertyChangeEvent}
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.widgets.Composite

import isabelle.eclipse.ui.util.SWTUtil.Disposable


/**
 * A mixin trait for SourceViewer that updates viewer font according to `fontKey`.
 * 
 * Tracks font changes in the preferences and updates accordingly.
 * 
 * @author Andrius Velykis
 */
trait FontUpdates {

  self: ISourceViewer =>

  // dispose when the source viewer goes
  self.getTextWidget onDispose disposeFontUpdates()

  def fontKey: String

  private val fontChangeListener = new IPropertyChangeListener {

    override def propertyChange(event: PropertyChangeEvent) =
      if (event.getProperty().equals(fontKey)) updateFont()
  }

  JFaceResources.getFontRegistry.addListener(fontChangeListener)
  updateFont()

  def disposeFontUpdates() {
    JFaceResources.getFontRegistry.removeListener(fontChangeListener)
  }

  private def updateFont() {
    val font = JFaceResources.getFont(fontKey)

    if (font != getTextWidget.getFont) {
      setFont(font)
    }
  }

  /**
   * Sets the font for the viewer sustaining selection and scroll position.
   */
  /* Adapted from AbstractTextEditor.setFont */
  private def setFont(font: Font): Unit = Option(getDocument) match {
    case Some(_) => {
      val selectionProvider = getSelectionProvider
      val selection = selectionProvider.getSelection
      val topIndex = getTopIndex

      val styledText = getTextWidget

      val parent = this match {
        case ext: ITextViewerExtension => ext.getControl
        case _ => styledText
      }

      parent.setRedraw(false)

      styledText.setFont(font)
      doSetFont(font)

      selectionProvider.setSelection(selection)
      setTopIndex(topIndex)

      parent match {
        case composite: Composite => composite.layout(true)
        case _ =>
      }

      parent.setRedraw(true)
    }

    case None => {
      getTextWidget.setFont(font)
      doSetFont(font)
    }
  }

  /**
   * Allows further font updates before the redraw is enabled again
   */
  def doSetFont(font: Font) {}

}
