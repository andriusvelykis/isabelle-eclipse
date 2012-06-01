package isabelle.eclipse.core.util

import isabelle.Event_Bus
import isabelle.Session
import isabelle.eclipse.core.IsabelleCorePlugin
import isabelle.eclipse.core.app.IIsabelleSessionListener
import scala.actors.Actor

/** Support for listening to session events.
  *
  * The session actor gets attached to the session when the session is initialised and
  * gets removed during sesion shutdown.
  * 
  * @author Andrius Velykis 
  */
trait SessionEvents {

  private val appListener = new IIsabelleSessionListener {

    override def systemInit = {}

    override def sessionInit(session: Session) = initSession(session)

    override def sessionShutdown(session: Session) = shutdownSession(session)
  }

  /** Initialiser, needs to be called to start listening to session events.
    * This is used to avoid attaching listeners in constructors.
    */
  protected def initSessionEvents() {
    // add listener to the isabelle app to react to session init
    val isabelle = IsabelleCorePlugin.getIsabelle
    isabelle.addSessionListener(appListener)
    
    Option(isabelle.getSession()) foreach initSession
  }
  
  /** Disconnects listeners from session events. */
  protected def disposeSessionEvents() {
    // remove isabelle app listener
    val isabelle = IsabelleCorePlugin.getIsabelle
    isabelle.removeSessionListener(appListener)
    
    Option(isabelle.getSession()) foreach shutdownSession
  }
  
  private def initSession(session: Session) {
    sessionEvents(session) foreach { _ += sessionActor }
  }
  
  private def shutdownSession(session: Session) {
    sessionEvents(session) foreach { _ -= sessionActor }
  }
  
  /** The actor to attach to the given event buses. */
  protected def sessionActor(): Actor
  
  /** Event buses to attach the actor to. */
  protected def sessionEvents(session: Session): List[Event_Bus[_]]
  
}
