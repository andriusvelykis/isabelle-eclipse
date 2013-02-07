package isabelle.eclipse.launch

import java.net.{MalformedURLException, URL}

import org.eclipse.jface.resource.ImageDescriptor


/**
 * Isabelle launch image definitions.
 *
 * When images are used in label providers (e.g. where Image) is required, they must be disposed manually.
 * For convenience, [[org.eclipse.jface.resource.ResourceManager]] could be used.
 *
 * @author Andrius Velykis
 */
object IsabelleLaunchImages {

  private lazy val ICON_BASE_URL = IsabelleLaunchPlugin.plugin.getBundle.getEntry("icons/")

  val MISSING_ICON = ImageDescriptor.getMissingImageDescriptor

  lazy val TAB_MAIN = create("main_tab.gif")
  lazy val TAB_INSTALLATION = create("isabelle.png")
  lazy val TAB_SESSION_DIRS = create("session-dirs.gif")

  lazy val SESSION = create("logic_obj.gif")

  private def create(iconPath: String) = {
    try {
      val url = new URL(ICON_BASE_URL, iconPath)
      ImageDescriptor.createFromURL(url)
    } catch {
      case _: MalformedURLException => MISSING_ICON
    }
  }

}
