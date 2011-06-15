
package isabelle.scala

import isabelle._

class DocumentStatusFacade(status : Isar_Document.Status) {

  def getStatus() = status

  def getForked() : Int = status match {
    case Isar_Document.Forked(forks) => forks
    case bad => -1
  }

  def isForked() = getForked >= 0

  def isUnprocessed() = status == Isar_Document.Unprocessed

  def isFinished() = status == Isar_Document.Finished

  def isFailed() = status == Isar_Document.Failed

}
