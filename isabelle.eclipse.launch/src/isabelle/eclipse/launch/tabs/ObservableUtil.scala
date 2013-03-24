package isabelle.eclipse.launch.tabs


/**
 * Utilities for observables and publisher/subscribe functionality
 * 
 * @author Andrius Velykis
 */
object ObservableUtil {

  /**
   * A wrapper for ObservableValue that adapts the value type.
   */
  class AdapterObservableValue[V, BaseV](base: ObservableValue[BaseV])(adapt: BaseV => V)
      extends ObservableValue[V] {
    
    // pass on the events
    base.subscribe(publish)
    
    override def value = adapt(base.value)
  }

}
