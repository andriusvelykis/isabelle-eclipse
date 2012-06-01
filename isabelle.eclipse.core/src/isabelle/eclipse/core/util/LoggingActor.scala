package isabelle.eclipse.core.util

import isabelle.eclipse.core.IsabelleCorePlugin
import scala.actors.Actor

/** A convenience actor that logs all exceptions to plug-in log.
  *
  * @author Andrius Velykis
  */
object LoggingActor {
  
  def apply(body: => Unit): Actor = {
    val a = new LoggingActor {
      def act() = body
    }
    a.start()
    a
  }
  
}

trait LoggingActor extends Actor {

  override def exceptionHandler: PartialFunction[Exception, Unit] = { case e: Exception => IsabelleCorePlugin.log(e) }
  
}
