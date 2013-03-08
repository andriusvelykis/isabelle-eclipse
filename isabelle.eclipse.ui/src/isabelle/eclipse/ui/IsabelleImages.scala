package isabelle.eclipse.ui

import java.net.{MalformedURLException, URL}

import org.eclipse.jface.resource.ImageDescriptor


/**
 * Isabelle image definitions.
 *
 * When images are used in label providers (e.g. where Image) is required, they must be disposed manually.
 * For convenience, [[org.eclipse.jface.resource.ResourceManager]] could be used.
 *
 * @author Andrius Velykis
 */
object IsabelleImages {

  private lazy val ICON_BASE_URL = IsabelleUIPlugin.getDefault.getBundle.getEntry("icons/")

  val MISSING_ICON = ImageDescriptor.getMissingImageDescriptor

  lazy val ISABELLE_FILE = create("isabelle_file.gif")
  lazy val ISABELLE_LOADED_FILE = create("isabelle_loaded_file.gif")
  
  lazy val RAW_OUTPUT_CONSOLE = create("isabelle.png")
  lazy val CONTENT_ASSIST = create("isabelle.png")
  lazy val OUTLINE_ITEM = create("isabelle.png")
  lazy val RAW_TREE = create("raw_tree.gif")
  lazy val SHOW_TRACE = create("show_trace.gif")


  private def create(iconPath: String) = {
    try {
      val url = new URL(ICON_BASE_URL, iconPath)
      ImageDescriptor.createFromURL(url)
    } catch {
      case _: MalformedURLException => MISSING_ICON
    }
  }

}
