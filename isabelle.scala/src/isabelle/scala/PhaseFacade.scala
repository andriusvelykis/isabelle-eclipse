
package isabelle.scala

import isabelle._

class PhaseFacade(phase : Session.Phase) {

  def getPhase() = phase

  def isFailed() = phase == Session.Failed

  def isReady() = phase == Session.Ready

  def isShutdown() = phase == Session.Shutdown

  def isStartup() = phase == Session.Startup

  def isInactive() = phase == Session.Inactive

}
