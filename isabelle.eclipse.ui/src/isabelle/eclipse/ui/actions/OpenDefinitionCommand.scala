package isabelle.eclipse.ui.actions

import org.eclipse.core.commands.{AbstractHandler, ExecutionEvent, ExecutionException}
import org.eclipse.jface.text.Region
import org.eclipse.ui.handlers.HandlerUtil

import isabelle.eclipse.ui.editors.{EditorUtil, TheoryEditor}
import isabelle.eclipse.ui.text.hyperlink.IsabelleHyperlinkDetector


/**
 * A command that opens definition of element under cursor in Isabelle Theory editor.
 * 
 * Reuses hyperlink detector and opens the first available hyperlink, if any.
 * 
 * @author Andrius Velykis
 */
class OpenDefinitionCommand extends AbstractHandler {

  @throws[ExecutionException]
  override def execute(event: ExecutionEvent): AnyRef = {

    HandlerUtil.getActiveEditor(event) match {
      case editor: TheoryEditor => openSelectedDefinition(editor)
      case _ =>
    }

    null
  }

  private def openSelectedDefinition(editor: TheoryEditor) =
    Option(EditorUtil.getTextViewer(editor)) match {

      case Some(textViewer) => {
        val caretPosition = editor.caretPosition
        val hyperlinkDetector = new IsabelleHyperlinkDetector(
          editor.isabelleModel map (_.snapshot),
          Option(editor.getSite) map (_.getPage))

        // reuse hyperlink detector at caret position
        val offsetRegion = new Region(caretPosition, 0)
        val hyperlinks = Option(hyperlinkDetector.detectHyperlinks(textViewer, offsetRegion, false))
        // get the first one
        val hyperlink = hyperlinks flatMap (_.headOption)

        // open the hyperlink
        hyperlink foreach (_.open())

        // cleanup
        hyperlinkDetector.dispose()
      }

      case _ =>
    }

}