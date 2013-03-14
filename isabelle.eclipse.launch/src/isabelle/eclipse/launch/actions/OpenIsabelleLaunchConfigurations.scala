package isabelle.eclipse.launch.actions

import org.eclipse.debug.ui.actions.OpenLaunchDialogAction

import isabelle.eclipse.launch.IsabelleLaunchConstants.ISABELLE_LAUNCH_GROUP


/**
 * Opens the launch config dialog on the Isabelle configurations launch group.
 */
class OpenIsabelleLaunchConfigurations extends OpenLaunchDialogAction(ISABELLE_LAUNCH_GROUP)
