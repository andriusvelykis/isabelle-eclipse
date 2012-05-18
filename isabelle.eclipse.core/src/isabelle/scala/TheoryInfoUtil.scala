
package isabelle.scala

import scala.collection.JavaConversions._

import isabelle._

object TheoryInfoUtil {

  def getDependencies(theoryInfo: Thy_Info, names: java.util.List[DocumentRef]): 
	  java.util.List[(DocumentRef, Document.Node_Header)] = {
    val namesScala = names.map(ref => ref.getRef())
    val deps = theoryInfo.dependencies(namesScala.toList).map{ t => (new DocumentRef(t._1), t._2) }
    deps
  }

}
