
package isabelle.scala

import isabelle._

class IsabelleSystemFacade (isabelleHome: String) {

  private val system = new Isabelle_System(isabelleHome)

  def getSystem(): Isabelle_System = system

  def findLogics(): Array[String] = system.find_logics().toArray;

  def getThyName(path : String) : String = {

    Thy_Header.split_thy_path(system.posix_path(path))
    match {
      case Some((dir, thy_name)) => dir + "/" + thy_name
      case None => ""
    }
  }

  def tryRead(paths : Array[String]) : String = system.try_read(paths)

}
