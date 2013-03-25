package isabelle.eclipse.launch.tabs

import java.util.concurrent.CopyOnWriteArraySet

import scala.collection.JavaConverters._


/**
 * A trait to support basic event publisher functions and allow retrieval of the value.
 * 
 * @author Andrius Velykis
 */
trait ObservableValue[V] {
  
  private val listeners = new CopyOnWriteArraySet[() => Unit]
  
  def subscribe(listener: () => Unit) = listeners.add(listener)
  
  def unsubscribe(listener: () => Unit) = listeners.remove(listener)
  
  def publish() = listeners.iterator.asScala foreach (l => l())
  
  def value: V
  
}
