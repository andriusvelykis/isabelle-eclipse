package isabelle.eclipse.ui.internal

import org.eclipse.core.runtime.{IStatus, Status}
import org.eclipse.swt.widgets.Display
import org.eclipse.ui.plugin.AbstractUIPlugin
import org.osgi.framework.BundleContext

import isabelle.eclipse.ui.preferences.IsabelleFontLoad


/**
 * @author Andrius Velykis
 */
object IsabelleUIPlugin {

  // The shared instance
  private var instance: IsabelleUIPlugin = _
  def plugin = instance

  /**
   * Logs the status into plug-in log.
   * 
   * @param status  status to log
   */
  def log(status: IStatus) = plugin.getLog.log(status)

  /**
   * Returns a new error `IStatus` for this plug-in.
   *
   * @param message    text to have as status message
   * @param exception  exception to wrap in the error `IStatus`
   * @return  the error `IStatus` wrapping the exception
   */
  def error(ex: Option[Throwable] = None, msg: Option[String] = None): IStatus = {

    // if message is not given, try to use exception's
    val message = msg orElse { ex.flatMap(ex => Option(ex.getMessage)) }

    new Status(IStatus.ERROR, plugin.pluginId, 0, message.orNull, ex.orNull);
  }
}


/**
 * @author Andrius Velykis
 */
class IsabelleUIPlugin extends AbstractUIPlugin {

  IsabelleUIPlugin.instance = this

  // The plug-in ID
  def pluginId = "isabelle.eclipse.ui" //$NON-NLS-1$

  @throws[Exception]
  override def start(context: BundleContext) {

    Option(Display.getCurrent) match {
      // already UI thread - use it without postponing
      // to avoid editors initialising with incorrect font
      case Some(_) => IsabelleFontLoad.loadIsabelleFont()

      // non-UI thread
      case None => Display.getDefault asyncExec new Runnable {
        override def run() = IsabelleFontLoad.loadIsabelleFont()
      }
    }
  }

}
