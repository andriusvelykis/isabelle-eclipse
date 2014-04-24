package isabelle.eclipse.launch.config

import java.io.File

/**
 * @author Andrius Velykis
 */
object AppBundleLaunch {

  /**
   * Adapts Isabelle installation path when MacOSX .app bundle is selected.
   *
   * The actual Isabelle installation directory is nested within the .app bundle, so the path must
   * be adjusted accordingly.
   */
  def adaptBundlePath(path: String): String = {

    val bundlePath = path + "/Isabelle/"

    val bundleFile = new File(bundlePath)
    if (!bundleFile.isDirectory) {
      // invalid - use the original
      path

    } else {
      bundlePath
    }
  }

}

/**
 * An extension of Isabelle directory-based launch that adapts the path to match MacOSX .app bundle
 * installation.
 * 
 * @author Andrius Velykis
 */
class AppBundleLaunch extends RootDirLaunch {

  // must be a directory, even though we are using FileDialog - .app bundle is actually a directory
  override def notDirectoryMessage =
    "Isabelle installation location is not an application bundle file"

  override def adaptInstallationPath(path: String): String =
    AppBundleLaunch.adaptBundlePath(path)

}
