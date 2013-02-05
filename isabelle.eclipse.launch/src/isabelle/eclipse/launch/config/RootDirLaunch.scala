package isabelle.eclipse.launch.config

import java.io.File

import scala.util.Either

import org.eclipse.core.runtime.IStatus
import org.eclipse.debug.core.ILaunchConfiguration

import isabelle.eclipse.launch.config.IsabelleLaunch._
import isabelle.eclipse.launch.config.LaunchConfigUtil._


/**
 * Generic Isabelle launch delegate for directory-based Isabelle installation
 * (e.g. Windows or Linux).
 * 
 * @author Andrius Velykis
 */
class RootDirLaunch extends IsabelleLaunch {

  override def installationPath(configuration: ILaunchConfiguration): Either[IStatus, String] =
    installationDir(configuration).right map (_.getPath)


  private def installationDir(configuration: ILaunchConfiguration): Either[IStatus, File] = {

    val locationConfig = configValue(configuration, IsabelleLaunchConstants.ATTR_LOCATION, "")

    if (locationConfig.isEmpty) {
      abort("Isabelle location not specified")
    } else {

      // allow subclasses to adapt the actual path
      val location = adaptInstallationPath(locationConfig)

      val file = new File(location)

      if (!file.exists) {
        abort("Isabelle location does not exist")

      } else if (!file.isDirectory) {
        // must be a directory
        abort(notDirectoryMessage)

      } else {
        // correct location
        result(file)
      }
    }
  }


  protected def adaptInstallationPath(path: String): String = path

  protected def notDirectoryMessage: String = "Isabelle installation location is not a directory"

}
