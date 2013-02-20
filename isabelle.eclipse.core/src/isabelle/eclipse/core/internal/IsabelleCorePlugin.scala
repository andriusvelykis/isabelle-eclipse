package isabelle.eclipse.core.internal

import org.eclipse.core.runtime.{IStatus, Plugin, Status}
import org.osgi.framework.BundleContext

import isabelle.eclipse.core.app.Isabelle


/**
 * @author Andrius Velykis
 */
object IsabelleCorePlugin {

  // The shared instance
  private var instance: IsabelleCorePlugin = _
  def plugin = instance

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
class IsabelleCorePlugin extends Plugin {

  IsabelleCorePlugin.instance = this

  // The plug-in ID
  def pluginId = "isabelle.eclipse.core" //$NON-NLS-1$


  var isabelleInit = false

  /**
   * The Isabelle prover instance (may be not started)
   */
  lazy val isabelle: Isabelle = {
    isabelleInit = true
    new Isabelle
  }

  override def stop(context: BundleContext) {

    if (isabelleInit) {
      isabelle.stop()
    }

    super.stop(context)
  }

}
