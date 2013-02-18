package isabelle.eclipse.launch.tabs

import scala.collection.mutable.Publisher


/**
 * Utilities for observables and publisher/subscribe functionality
 * 
 * @author Andrius Velykis
 */
object ObservableUtil {

  /**
   * An implicit class that adds a simpler subscribe method to publisher 
   */
  implicit class NotifyPublisher[Evt](pub: Publisher[Evt]) {

    def subscribeFun(sub: Evt => Unit) {
      pub.subscribe(new pub.Sub {
        override def notify(p: pub.Pub, event: Evt) = sub(event)
      })
    }
  }

}
