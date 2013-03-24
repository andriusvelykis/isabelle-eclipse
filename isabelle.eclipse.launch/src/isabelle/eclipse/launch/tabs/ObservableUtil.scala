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


  /**
   * A wrapper for Publisher that adapts the event type.
   */
  class AdapterPublisher[Evt, BaseEvt](base: Publisher[BaseEvt])(adapt: BaseEvt => Evt)
      extends Publisher[Evt] {

    // subscribe to base listener
    base.subscribe(new base.Sub {
      override def notify(p: base.Pub, event: BaseEvt) = publish(adapt(event))
    })
  }


  /**
   * A wrapper for ObservableValue that adapts the value type.
   */
  class AdapterObservableValue[V, BaseV](base: ObservableValue[BaseV])(adapt: BaseV => V)
      extends AdapterPublisher[V, BaseV](base)(adapt) with ObservableValue[V] {
    
    override def value = adapt(base.value)
  }

}
