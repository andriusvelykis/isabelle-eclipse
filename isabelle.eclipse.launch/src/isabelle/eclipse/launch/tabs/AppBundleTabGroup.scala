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

    val appBundleSelect = new AppBundleSelectComponent with ObservableValue[Option[String]] {
      override def value = selectedDirInAppBundle
    }
    
    val sessionDirs = new DirListComponent with ObservableValue[Seq[String]] {
      override def value = selectedDirs
    }
    
    val envTab = new IsabelleEnvironmentTab

    // no additional system properties by default
    val emptySystemProps = new ObservableValue[Map[String, String]] {
      override def value = Map()
    }
    
    // use the selected directory directly as Isabelle path
    val sessionSelect = new SessionSelectComponent(
        appBundleSelect, sessionDirs, envTab, emptySystemProps)
    
    

    val tabs = Array[ILaunchConfigurationTab](
      new IsabelleMainTab(List(appBundleSelect, sessionSelect)),
      new SessionDirsTab(List(sessionDirs)),
      envTab,
      new IsabelleBuildTab,
      new CommonTab)

    setTabs(tabs)
  }
}
