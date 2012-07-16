package isabelle.eclipse.core.app

import scala.actors.Actor._

import isabelle.Event_Bus
import isabelle.Isabelle_System
import isabelle.Session
import isabelle.Thy_Info
import isabelle.Thy_Load
import isabelle.eclipse.core.resource.URIThyLoad


/** The central Isabelle system class, which allows starting/stopping the prover
  * 
  * @author Andrius Velykis 
  */
object Isabelle {
  
  sealed abstract class IsabelleSystemEvent
  case object SystemInit extends IsabelleSystemEvent
  case class SessionInit(session: Session) extends IsabelleSystemEvent
  case class SessionShutdown(session: Session) extends IsabelleSystemEvent
  
}

class Isabelle {
  
  import Isabelle._

  // event bus to subscribe to system events, e.g. init/session load
  val systemEvents = new Event_Bus[IsabelleSystemEvent]
  
  lazy val thyLoad: Thy_Load = new URIThyLoad
  lazy val thyInfo: Thy_Info = new Thy_Info(thyLoad)
  
  private var systemInit = false
  /** checks whether the system is initialised (session may not be started yet, but symbols may be available) */
  def isInit = systemInit
  
  private var currentSession: Option[Session] = None
  /** Retrieves the current session - may not be available if Isabelle has not been started */
  def session() = currentSession
  
  /** Running if session is available and ready */
  def isRunning = session.map(_.phase == Session.Ready).getOrElse(false)
  
  
  def start(isabellePath: String, logic: String): Session = {

    // TODO check paths for the same system?
    // TODO switch logic? E.g. dispose the old session and start a new one
    if (isInit) {
      throw new IllegalStateException("Isabelle already initialised!")
    }
    
    Isabelle_System.init(isabellePath)
    systemInit = true
    // notify about system init
    systemEvents.event(SystemInit)
    
    startSession(logic)
  }
  
  private def startSession(logic: String): Session = {    
    
    val s = new Session(thyLoad)
    currentSession = Some(s)
    s.phase_changed += sessionManager
    
    // start the session
    // TODO adjust timeouts?
    // TODO accept arguments from some configuration?
    val modes = List("-mxsymbols"/*, "-mno_brackets", "-mno_type_brackets"*/)
    s.start(modes ::: List(logic))
    
    s
  }

  def stop() {
    currentSession.foreach(_.stop)
    systemInit = false
  }

  private def shutdownSession() {
    currentSession foreach { s =>
      // notify subscribers about the session shutdown
      systemEvents.event(SessionShutdown(s))

      // disconnect the manager
      s.phase_changed -= sessionManager
    }
    
    currentSession = None
  }

  private val sessionManager = actor {
    loop {
      react {
        case phase: Session.Phase => phase match {

          case Session.Ready =>
            systemEvents.event(SessionInit(session.get))

          case Session.Shutdown => shutdownSession()

          case _ =>
        }
        case _ => // ignore other events
      }
    }
  }
  
}
