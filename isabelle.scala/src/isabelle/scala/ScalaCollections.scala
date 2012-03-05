package isabelle.scala

import scala.collection.JavaConversions._

object ScalaCollections {

  /**
   * Creates an immutable Scala List from the given collection. The collection
   * is copied to the list.
   * 
   * @param col the collection to convert
   * @return immutable Scala List containing elements from the given collection
   */
  def toScalaList[E](col : java.util.Collection[_ <: E]) : List[E] = col.toList
 
  def singletonList[E](el : E) : List[E] = List(el)
  
  def emptyList[E]() : List[E] = Nil
  
}