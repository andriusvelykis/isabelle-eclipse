package isabelle.eclipse.core.util

import scala.actors.Actor
import scala.actors.Actor._

import isabelle.Event_Bus
import isabelle.Session
import isabelle.eclipse.core.IsabelleCore
import isabelle.eclipse.core.app.Isabelle


/** Support for listening to session events.
  *
  * The session actor gets attached to the session when the session is initialised and
  * gets removed during sesion shutdown.
  * 
  * @author Andrius Velykis 
  */
trait SessionEvents {

  /** a listener for system events, which attaches/detaches specific session listeners upon session init/shutdown  */
  private val systemListener = LoggingActor {
    loop {
      react {
        case Isabelle.SystemInit => systemInit()
        case Isabelle.SessionInit(session) => initSession(session)
        case Isabelle.SessionShutdown(session) => shutdownSession(session)
        case _ =>
      }
    }
  }

  /** Initialiser, needs to be called to start listening to session events.
    * This is used to avoid attaching listeners in constructors.
    */
  protected def initSessionEvents() {
    // add listener to the isabelle app to react to session init
    val isabelle = IsabelleCore.isabelle
    isabelle.systemEvents += systemListener
    
    if (isabelle.isInit) {
      systemInit()
    }
    
    isabelle.session foreach initSession
  }
  
  /** Disconnects listeners from session events. */
  protected def disposeSessionEvents() {
    // remove isabelle app listener
    val isabelle = IsabelleCore.isabelle
    isabelle.systemEvents -= systemListener
    
    isabelle.session foreach shutdownSession
  }
  
  private def initSession(session: Session) {
    sessionEvents(session) foreach { _ += sessionActor }
    
    sessionInit(session)
  }
  
  private def shutdownSession(session: Session) {
    sessionEvents(session) foreach { _ -= sessionActor }
    
    sessionShutdown(session)
  }
  
  /** The actor to attach to the given event buses. */
  protected def sessionActor(): Actor
  
  /** Event buses to attach the actor to. */
  protected def sessionEvents(session: Session): List[Event_Bus[_]]
  
  protected def systemInit() {}
  
  protected def sessionInit(session: Session) {}
  
  protected def sessionShutdown(session: Session) {}
  
}
