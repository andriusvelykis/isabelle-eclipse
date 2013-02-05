package isabelle.eclipse.launch

import org.eclipse.core.runtime.{IStatus, Status}
import org.eclipse.ui.plugin.AbstractUIPlugin


/** 
 * @author Andrius Velykis
 */
object IsabelleLaunchPlugin {

  // The shared instance
  private var instance: IsabelleLaunchPlugin = _
  def plugin = instance

  def log(status: IStatus) = plugin.getLog.log(status)

  /** Returns a new error `IStatus` for this plug-in.
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

class IsabelleLaunchPlugin extends AbstractUIPlugin {

  IsabelleLaunchPlugin.instance = this
  
  // The plug-in ID
  def pluginId = "isabelle.eclipse.launch" //$NON-NLS-1$
  
}
