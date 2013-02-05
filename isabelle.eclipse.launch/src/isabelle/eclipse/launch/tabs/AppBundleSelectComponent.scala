package isabelle.eclipse.launch.tabs

import java.io.File

import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.FileDialog

import isabelle.eclipse.launch.config.AppBundleLaunch


/**
 * A specialization of Isabelle directory selection component for MacOSX application
 * bundles (.app).
 * 
 * @author Andrius Velykis
 */
class AppBundleSelectComponent extends DirSelectComponent {

  /**
   * Retrieves Isabelle's installation directory translated from the selected App bundle
   */
  def selectedDirInAppBundle: Option[String] = 
    selectedDir map AppBundleLaunch.adaptBundlePath
  
  
  override protected def locationLabel = "Isabelle application bundle (.app) location:"

  override protected def defaultLocationMessage =
    "Please specify the location of the Isabelle application bundle (.app file) you would like to configure"

  override protected def notDirectoryMessage =
    "Isabelle installation location specified is not an application bundle file"
 
  override protected def browseDir(): Option[String] = {
    // Use file selection dialog for bundle file
    val fileDialog = new FileDialog(shell, SWT.NONE)
    fileDialog.setText("Select a Isabelle application bundle (.app)")
    selectedDir foreach fileDialog.setFileName
    
    Option(fileDialog.open)
  }
  
  override def isValid(configuration: ILaunchConfiguration,
                       newConfig: Boolean): Option[Either[String, String]] = {
    
    val error = super.isValid(configuration, newConfig)
    
    error orElse validateBundleContents(newConfig)
  }

  /**
   * Checks whether the adapted bundle path is still a directory
   */
  private def validateBundleContents(newConfig: Boolean): Option[Either[String, String]] = {

    val location = selectedDirInAppBundle.get

    val file = new File(location)

    if (!file.isDirectory) {
      if (newConfig) {
        Some(Right(defaultLocationMessage))
      } else {
        Some(Left("Invalid Isabelle bundle selected: cannot locate Isabelle directory at " + location))
      }
    } else {
      // valid location - no errors
      None
    }
  }
  
}
