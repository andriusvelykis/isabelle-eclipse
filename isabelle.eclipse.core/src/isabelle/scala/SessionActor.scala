
package isabelle.scala

import scala.collection.JavaConversions._
import scala.actors.Actor
import Actor._

import isabelle._

class SessionActor() {

  private var phaseListener : Option[ISessionPhaseListener] = None
  private var rawMessageListener : Option[ISessionRawMessageListener] = None
  private var commandsListener : Option[ISessionCommandsListener] = None

  def phaseChanged(listener : ISessionPhaseListener) : SessionActor = {
    phaseListener = Some(listener)
    this
  }

  def rawMessages(listener : ISessionRawMessageListener) : SessionActor = {
    rawMessageListener = Some(listener)
    this
  }

  def commandsChanged(listener : ISessionCommandsListener) : SessionActor = {
    commandsListener = Some(listener)
    this
  }
  
  protected def handleException(e : Exception): Unit = {
    throw e
  }

  private val mainActor = {
    val a = new Actor {
      
      def act() =
        loop {
          react {
            case phase: Session.Phase => {
              phaseListener match {
                case Some(listener) => listener.phaseChanged(phase);
                case None =>
              }
            }

            case result: Isabelle_Process.Result => {
              rawMessageListener match {
                case Some(listener) => listener.handleMessage(result);
                case None =>
              }
            }

            case changed: Session.Commands_Changed => {
              commandsListener match {
                case Some(listener) => listener.commandsChanged(changed);
                case None =>
              }
            }

            case bad => System.err.println("Phase actor: ignoring bad message " + bad)
          }
        }

      override def exceptionHandler = {
        case e: Exception => handleException(e)
      }
    }
    a.start()
    a

  }

  private[scala] def getActor() = mainActor


}

trait ISessionPhaseListener {

  def phaseChanged(phase : Session.Phase)

}

trait ISessionRawMessageListener {

  def handleMessage(result : Isabelle_Process.Result)

}

trait ISessionCommandsListener {

  def commandsChanged(changed : Session.Commands_Changed)

}
