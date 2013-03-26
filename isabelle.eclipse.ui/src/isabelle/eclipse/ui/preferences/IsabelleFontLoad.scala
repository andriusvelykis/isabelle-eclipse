package isabelle.eclipse.ui.preferences

import scala.util.{Failure, Success, Try}

import org.eclipse.core.runtime.{FileLocator, Path}
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Display

import isabelle.eclipse.ui.internal.IsabelleUIPlugin.{error, log, plugin}


/**
 * Loads IsabelleText font from the plug-in if it is not available in the system.
 * 
 * This allows to use Isabelle fonts within the plug-ins without requiring them to be installed
 * on user's computer.
 * 
 * @author Andrius Velykis
 */
object IsabelleFontLoad {

  def ISABELLE_FONT_NAME = "IsabelleText"


  /**
   * Loads Isabelle font to be available for SWT widgets, if one does not exists in the system
   * already.
   */
  def loadIsabelleFont() {

    val display = Display.getCurrent

    // first check if any of the Isabelle fonts already exist in the system
    val fonts = display.getFontList(null, true)
    
    val fontName = ISABELLE_FONT_NAME
    val existingIsabelleFonts = fonts.toStream filter (_.getName == fontName)
    
    val hasPlain = existingIsabelleFonts exists (_.getStyle() == SWT.NONE)
    val hasBold = existingIsabelleFonts exists (_.getStyle() == SWT.BOLD)

    // load necessary fonts
    if (!hasPlain) loadFont(display, "IsabelleText.ttf")
    if (!hasBold) loadFont(display, "IsabelleTextBold.ttf")
  }


  private def loadFont(display: Display, fontName: String): Boolean = {
    
    def failed(msg: String, ex: Option[Throwable] = None): Boolean = {
      log(error(ex, Some(msg)))
      false
    }

    // resolve font URL within the plug-in bundle (note, the font may be inside a JAR here)
    val fontUrl = fontBundleUrl(fontName)

    fontUrl match {
      case None =>
        failed("Font " + fontName + " cannot be found in the plug-in.")

      // get file URL (this will also extract the font file if needed)
      case Some(fontUrl) =>
        Try(FileLocator.toFileURL(fontUrl)) match {

          case Failure(ex) =>
            failed("Font " + fontName + " cannot be loaded: " + ex.getMessage, Some(ex))

          case Success(fontFileUrl) => {

            // get the file path and try loading the font
            val fontFilePath = new Path(fontFileUrl.getPath).toOSString
            val loaded = display.loadFont(fontFilePath)

            if (!loaded) {
              failed("Font " + fontName + " cannot be loaded from file " + fontFilePath)
            } else {
              true
            }
          }
        }
    }
  }
  
  private def fontBundleUrl(fontName: String) =
    Option(plugin.getBundle.getEntry("fonts/" + fontName))

}
