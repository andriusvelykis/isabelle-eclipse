
package isabelle.scala

import scala.collection.JavaConversions._
import scala.actors.Actor
import Actor._

import isabelle._
import isabelle.scala.SessionEventType._

object SessionUtil {

  def addSessionEventActor(session : Session, eventType : SessionEventType, actor : SessionActor) {
    getSessionEventBus(session, eventType) += actor.getActor
  }
  
  def removeSessionEventActor(session : Session, eventType : SessionEventType, actor : SessionActor) {
    getSessionEventBus(session, eventType) -= actor.getActor
  }
  
  private def getSessionEventBus(session : Session, eventType : SessionEventType) : Event_Bus[_] = {
    eventType match {
      case COMMAND => session.commands_changed
      case PHASE => session.phase_changed
      case RAW_MESSAGES => session.raw_output_messages
    }
  }

}
