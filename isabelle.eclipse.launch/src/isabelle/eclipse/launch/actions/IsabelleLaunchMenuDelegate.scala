package isabelle.eclipse.launch.actions

import org.eclipse.debug.ui.DebugUITools
import org.eclipse.debug.ui.actions.AbstractLaunchToolbarAction
import org.eclipse.jface.action.IAction
import org.eclipse.jface.viewers.StructuredSelection
import org.eclipse.swt.widgets.Shell
import org.eclipse.ui.PlatformUI

import isabelle.eclipse.launch.IsabelleLaunchConstants.ISABELLE_LAUNCH_GROUP


/**
 * This action delegate is responsible for producing the Run > Isabelle sub menu contents, which
 * includes items to run last tool, favorite tools, and show the Isabelle launch configurations
 * dialog.
 *
 * @author Andrius Velykis
 */
class IsabelleLaunchMenuDelegate extends AbstractLaunchToolbarAction(ISABELLE_LAUNCH_GROUP) {

  override protected def getOpenDialogAction(): IAction = {
    val action = new OpenIsabelleLaunchConfigurations
    action.setActionDefinitionId("isabelle.eclipse.launch.openIsabelleConfigurations")
    action
  }

  /**
   * Launch the last launch, or open the launch config dialog if none.
   *
   * No context launching (see super to reinstate).
   */
  override def run(action: IAction) = Option(getLastLaunch()) match {
    case Some(config) =>
      DebugUITools.launch(config, getMode)

    case None =>
      DebugUITools.openLaunchConfigurationDialogOnGroup(
        shell.orNull, new StructuredSelection, getLaunchGroupIdentifier)
  }

  private def shell: Option[Shell] = {
    val workbench = PlatformUI.getWorkbench

    val window = Option(workbench.getActiveWorkbenchWindow) orElse
      workbench.getWorkbenchWindows.headOption

    window map (_.getShell)
  }
}
