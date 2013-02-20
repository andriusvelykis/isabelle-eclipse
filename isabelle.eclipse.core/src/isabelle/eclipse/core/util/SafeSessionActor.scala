package isabelle.eclipse.core.util

import isabelle.scala.SessionActor
import isabelle.eclipse.core.internal.IsabelleCorePlugin.{error, log}


/**
 * A session actor that handles all exceptions by logging them to plug-in log
 *
 * @author Andrius Velykis
 */
class SafeSessionActor extends SessionActor {

  // log exception (parent rethrows it)
  override def handleException(e: Exception) = log(error(Some(e)))

}
