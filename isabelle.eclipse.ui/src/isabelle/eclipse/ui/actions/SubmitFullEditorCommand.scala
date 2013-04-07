package isabelle.eclipse.ui.actions

import org.eclipse.core.commands.{AbstractHandler, ExecutionEvent, ExecutionException}
import org.eclipse.ui.handlers.HandlerUtil

import isabelle.eclipse.ui.editors.TheoryEditor


/**
 * A command submits the full contents of the currently open editor to the prover.
 * 
 * @author Andrius Velykis
 */
class SubmitFullEditorCommand extends AbstractHandler {

  @throws[ExecutionException]
  override def execute(event: ExecutionEvent): AnyRef = {

    HandlerUtil.getActiveEditor(event) match {
      case editor: TheoryEditor => editor.submitFull()
      case _ =>
    }

    null
  }
}