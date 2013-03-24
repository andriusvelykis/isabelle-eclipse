package isabelle.eclipse.launch.tabs

import org.eclipse.debug.ui.{
  AbstractLaunchConfigurationTabGroup,
  CommonTab,
  ILaunchConfigurationDialog,
  ILaunchConfigurationTab
}

import isabelle.eclipse.launch.config.WinDirLaunch

import ObservableUtil.AdapterObservableValue


/**
 * Isabelle launch configuration tabs for Windows Isabelle installation.
 * 
 * This is a directory-based installation with Cygwin configuration as well.
 * 
 * @author Andrius Velykis
 */
class WinDirTabGroup extends AbstractLaunchConfigurationTabGroup {

  override def createTabs(dialog: ILaunchConfigurationDialog, mode: String) {

    val dirSelect = new DirSelectComponent with ObservableValue[Option[String]] {
      override def value = selectedDir
    }

    val cygwinSelect =
      new CygwinDirSelectComponent(dirSelect) with ObservableValue[Option[String]] {
        override def value = selectedDir
      }
    
    val sessionDirs = new DirListComponent with ObservableValue[Seq[String]] {
      override def value = selectedDirs
    }

    // wrap the selected Cygwin path into system properties map
    val systemProps = new AdapterObservableValue(cygwinSelect)({ dir =>
      (dir map WinDirLaunch.cygwinSystemProperties) getOrElse Map()
    })
    
    val envTab = new IsabelleEnvironmentTab
    
    // use the selected directory directly as Isabelle path
    val sessionSelect = new SessionSelectComponent(dirSelect, sessionDirs, envTab, systemProps)
    
    val tabs = Array[ILaunchConfigurationTab](
      new IsabelleMainTab(List(dirSelect, cygwinSelect, sessionSelect)),
      new SessionDirsTab(List(sessionDirs)),
      envTab,
      new IsabelleBuildTab,
      new CommonTab)

    setTabs(tabs)
  }
}
