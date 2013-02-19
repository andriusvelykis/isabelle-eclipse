package isabelle.eclipse.launch.config

import scala.actors.Actor._
import scala.concurrent.{Await, Promise}
import scala.concurrent.duration.Duration

import isabelle.Session


/**
 * A one-shot tracker of the session phase - returns when a phase matches a requested.
 *
 * @author Andrius Velykis
 */
object PhaseTracker {

  /**
   * Suspends the thread execution until a desired phase is received from the session.
   *
   * The session loading is done asynchronously, so we need to suspend execution until the
   * result is received.
   */
  def waitForPhaseResult(session: Session, matchPhases: Set[Session.Phase]): Session.Phase = {
    
    val phasePromise = Promise[Session.Phase]()

    val sessionManager = actor {
      loop {
        react {
          case phase: Session.Phase if (matchPhases.contains(phase)) => {
            phasePromise.success(phase)
            exit()
          }
          case _ => // ignore other events
        }
      }
    }
    
    session.phase_changed += sessionManager
    // handle the current phase - pass it to the session manager
    sessionManager ! session.phase
    
    // await on the future and block until the required phase is reached - then return it
    val future = phasePromise.future
    val resultPhase = Await.result(future, Duration.Inf)
    
    // also remove the listener
    session.phase_changed -= sessionManager
    
    resultPhase
  }
  
}
