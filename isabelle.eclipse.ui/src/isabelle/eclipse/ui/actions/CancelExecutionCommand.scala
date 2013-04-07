package isabelle.eclipse.ui.actions

import org.eclipse.core.commands.{AbstractHandler, ExecutionEvent, ExecutionException}

import isabelle.eclipse.core.IsabelleCore


/**
 * A command that cancels the current Isabelle execution.
 * 
 * @author Andrius Velykis
 */
class CancelExecutionCommand extends AbstractHandler {

  @throws[ExecutionException]
  override def execute(event: ExecutionEvent): AnyRef = {

    // TODO cancel the build job as well via this command?
    IsabelleCore.isabelle.session foreach { _.cancel_execution() }

    null
  }

}
