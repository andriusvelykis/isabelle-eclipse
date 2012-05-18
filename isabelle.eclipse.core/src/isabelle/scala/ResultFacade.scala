
package isabelle.scala

import isabelle._

class ResultFacade(result : Isabelle_Process.Result) {

  def getResult() = result

//  def getMessageString() : String = XML.content(result.message).mkString

  def getMessageString() : String = result.message.toString

  def body() = result.body.toString

  def propertiesString() : String = result.properties.toString

}
