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

    val isaPath = new IsabellePathsObservableValue(dirSelect, Some(cygwinSelect))

    val sessionDirs = new DirListComponent with ObservableValue[Seq[String]] {
      override def value = selectedDirs
    }
    
    // use the selected directory directly as Isabelle path
    val sessionSelect = new SessionSelectComponent(isaPath, sessionDirs)
    
    val tabs = Array[ILaunchConfigurationTab](
      new IsabelleMainTab(List(dirSelect, cygwinSelect, sessionSelect)),
      new SessionDirsTab(List(sessionDirs)),
      new IsabelleBuildTab,
      new CommonTab)

    setTabs(tabs)
  }
}
