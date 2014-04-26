package isabelle.eclipse.ui.internal

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

  private lazy val ICON_BASE_URL = IsabelleUIPlugin.plugin.getBundle.getEntry("icons/")

  val MISSING_ICON = ImageDescriptor.getMissingImageDescriptor

  lazy val ISABELLE_FILE = create("isabelle_file.gif")
  lazy val ISABELLE_LOADED_FILE = create("isabelle_loaded_file.gif")
  
  lazy val RAW_OUTPUT_CONSOLE = create("isabelle_console_black.png")
  lazy val PROTOCOL_CONSOLE = create("isabelle_console.png")
  lazy val SYSLOG_CONSOLE = create("isabelle_console_red.png")
  lazy val CONTENT_ASSIST = create("isabelle.png")

  lazy val RAW_TREE = create("raw_tree.gif")
  lazy val SHOW_TRACE = create("show_trace.gif")

  lazy val HEADING = create("heading.png")
  lazy val LEMMA = create("lemma.png")
  lazy val SUCCESS = create("success.gif")
  lazy val TEXT = create("text.png")
  lazy val COMMAND_APPLY = create("command_apply.png")
  lazy val COMMAND_PROOF = create("command_proof.png")
  lazy val ISABELLE_ITEM = create("isabelle_item.png")

  lazy val PROGRESS = create("progress.png")


  private def create(iconPath: String) = {
    try {
      val url = new URL(ICON_BASE_URL, iconPath)
      ImageDescriptor.createFromURL(url)
    } catch {
      case _: MalformedURLException => MISSING_ICON
    }
  }

}
