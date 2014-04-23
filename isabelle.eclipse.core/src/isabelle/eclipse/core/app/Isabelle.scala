package isabelle.eclipse.core.app

import scala.actors.Actor._
import scala.util.Try

import org.eclipse.core.runtime.IPath

import isabelle.Event_Bus
import isabelle.Outer_Syntax
import isabelle.Session
import isabelle.eclipse.core.app.IsabelleBuild.IsabellePaths
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
  
  
  /** checks whether the system is initialised (session may not be started yet, but symbols may be available) */
  def isInit = IsabelleBuild.isInit
  
  private var currentSession: Option[Session] = None
  /** Retrieves the current session - may not be available if Isabelle has not been started */
  def session = currentSession

  def recentSyntax(): Option[Outer_Syntax] = session flatMap { s =>
    if (s.recent_syntax == Outer_Syntax.empty) None
    else Some(s.recent_syntax)
  }
  
  
  /** Running if session is available and ready */
  def isRunning = session.map(_.phase == Session.Ready).getOrElse(false)

  def start(isabellePath: IsabellePaths,
            sessionName: String,
            moreSessionDirs: Seq[IPath] = Nil): Try[Session] = {

    // start session if system init is successful
    val sessionTry = for {
      _ <- IsabelleBuild.init(isabellePath) map { _ =>
        systemEvents.event(SystemInit)
      }
      session <- startSession(moreSessionDirs, sessionName)
    } yield session
    
    // if init successful, mark the session as current
    sessionTry foreach { session => currentSession = Some(session) }
    
    sessionTry
  }
  
  
  private def startSession(moreSessionDirs: Seq[IPath], sessionName: String): Try[Session] = {    
    
    val contentTry = IsabelleBuild.sessionContent(moreSessionDirs, sessionName, false)
    
    contentTry map { content =>

      val thyLoad = new URIThyLoad(content.loaded_theories, content.syntax)
      
      val s = new Session(thyLoad)
      s.phase_changed += sessionManager
      
      // start the session
      // TODO adjust timeouts?
      // TODO accept arguments from some configuration?
      val modes = List("-mxsymbols"/*, "-mno_brackets", "-mno_type_brackets"*/)
      s.start(modes ::: List(sessionName))
      
      s
    }
  }

  def stop() {
    currentSession.foreach(_.stop)
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
