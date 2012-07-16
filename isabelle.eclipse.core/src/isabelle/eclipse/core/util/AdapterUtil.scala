package isabelle.eclipse.core.util

import org.eclipse.core.runtime.IAdaptable

/** Utilities for adapter methods.
  * 
  * @author Andrius Velykis 
  */
object AdapterUtil {

  /** A convenience adapter method to avoid writing typecast everywhere */
  def adapt[T](f: Class[_ >: T] => AnyRef)(arg: Class[T]): Option[T] = {
    Option(f(arg).asInstanceOf[T])
  }


  /** A convenience adapter for IAdaptable that takes care of typecasting */
  def adapt[T](adaptable: IAdaptable)(arg: Class[T]): Option[T] = {
    adapt(adaptable.getAdapter _)(arg)
  }
  
}
