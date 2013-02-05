package isabelle.eclipse.launch.tabs

import org.eclipse.debug.ui.{
  AbstractLaunchConfigurationTabGroup,
  CommonTab,
  ILaunchConfigurationDialog,
  ILaunchConfigurationTab
}

/**
 * Isabelle launch configuration tabs for MacOSX app bundle (.app) based Isabelle installation.
 * 
 * @author Andrius Velykis
 */
class AppBundleTabGroup extends AbstractLaunchConfigurationTabGroup {

  override def createTabs(dialog: ILaunchConfigurationDialog, mode: String) {

    val appBundleSelect = new AppBundleSelectComponent()
    // use the selected directory directly as Isabelle path
    val sessionSelect = new SessionSelectComponent(
      appBundleSelect,
      () => appBundleSelect.selectedDirInAppBundle)

    val tabs = Array[ILaunchConfigurationTab](
      new IsabelleMainTab(List(appBundleSelect, sessionSelect)),
      new CommonTab())

    setTabs(tabs)
  }
}
