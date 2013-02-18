package isabelle.eclipse.launch.tabs

import scala.collection.mutable.Publisher


/**
 * A trait to supplement Publisher with a value it contains notifies about.
 * 
 * @author Andrius Velykis
 */
trait ObservableValue[V] extends Publisher[V] {
  
  def value: V
  
}
