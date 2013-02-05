package isabelle.eclipse.launch.tabs

import org.eclipse.debug.ui.{
  AbstractLaunchConfigurationTabGroup,
  CommonTab,
  ILaunchConfigurationDialog,
  ILaunchConfigurationTab
}

/**
 * Isabelle launch configuration tabs for directory-based Isabelle installation.
 * 
 * @author Andrius Velykis
 */
class RootDirTabGroup extends AbstractLaunchConfigurationTabGroup {

  override def createTabs(dialog: ILaunchConfigurationDialog, mode: String) {

    val dirSelect = new DirSelectComponent()
    // use the selected directory directly as Isabelle path
    val sessionSelect = new SessionSelectComponent(dirSelect, () => dirSelect.selectedDir)

    val tabs = Array[ILaunchConfigurationTab](
      new IsabelleMainTab(List(dirSelect, sessionSelect)),
      new CommonTab())

    setTabs(tabs)
  }
}
