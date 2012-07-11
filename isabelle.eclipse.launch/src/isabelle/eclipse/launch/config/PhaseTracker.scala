package isabelle.eclipse.launch.config

import scala.actors.Actor._

import isabelle.Future
import isabelle.Promise
import isabelle.Session

/** A one-shot tracker of the session phase - returns the phase when the session
  * becomes either Failed or Ready.
  * 
  * 
  * @author Andrius Velykis 
  */
object PhaseTracker {
  
  /** Suspends the thread execution until a desired phase is received from the session.
    * The method expects Ready or Failed phases, indicating either a successful launch, or failure.
    * 
    * The session loading is done asynchronously, so we need to suspend execution until the
    * result is received.
	*/
  def waitForPhaseResult(session: Session): Session.Phase = {
    
    val phasePromise: Promise[Session.Phase] = Future.promise
    
    val sessionManager = actor {
      loop {
        react {
          case phase: Session.Phase => phase match {
            // only interested in Failed or Ready phases
            case Session.Failed | Session.Ready => phasePromise.fulfill(phase)
            case _ =>
          }
          case _ => // ignore other events
        }
      }
    }
    
    session.phase_changed += sessionManager
    // handle the current phase - pass it to the session manager
    sessionManager ! session.phase
    
    // join on the future and block until the required phase is reached - then return it
    val res = phasePromise.join
    
    // also remove the listener
    session.phase_changed -= sessionManager
    
    res
  }
  
}
