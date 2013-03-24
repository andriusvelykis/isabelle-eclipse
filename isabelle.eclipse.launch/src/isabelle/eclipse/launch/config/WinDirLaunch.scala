package isabelle.eclipse.launch.config

import java.io.File

import org.eclipse.core.runtime.IStatus
import org.eclipse.debug.core.ILaunchConfiguration

import isabelle.eclipse.launch.config.IsabelleLaunch._
import isabelle.eclipse.launch.config.LaunchConfigUtil.configValue

/**
 * @author Andrius Velykis
 */
object WinDirLaunch {

  /**
   * Retrieves Isabelle Cygwin path (expected to be within Isabelle installation directory)
   */
  def isabelleCygwinPath(isabellePath: String): Option[String] = {

    val slash = File.separator
    val cygwinPath = new File(new File(isabellePath), "contrib/cygwin")
    
    if (!cygwinPath.isDirectory) {
      // invalid
      None
    } else {
      Some(cygwinPath.toString)
    }
  }

  /**
   * Creates system properties map with the given cygwin path
   */
  def cygwinSystemProperties(cygwinPath: String): Map[String, String] =
    Map("cygwin.root" -> cygwinPath)

}

/**
 * An extension of Isabelle directory-based launch that adapts the path to match MacOSX .app bundle
 * installation.
 *
 * @author Andrius Velykis
 */
class WinDirLaunch extends RootDirLaunch {

  override def systemProperties(configuration: ILaunchConfiguration):
      Either[IStatus, Map[String, String]] = {

    val cygwinConfig = configValue(configuration, IsabelleLaunchConstants.ATTR_CYGWIN_LOCATION, "")

    if (cygwinConfig.isEmpty) {
      abort("Cygwin location not specified")
    } else {

      val file = new File(cygwinConfig)

      if (!file.exists) {
        abort("Cygwin location does not exist")

      } else if (!file.isDirectory) {
        // must be a directory
        abort("Cygwin location is not a directory")

      } else {
        // correct location
        result(WinDirLaunch.cygwinSystemProperties(file.toString))
      }
    }
  }
}
