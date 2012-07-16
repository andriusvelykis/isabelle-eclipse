package isabelle.eclipse.core.util

import isabelle.Session
import isabelle.Event_Bus

abstract class SessionEventSupport[T] extends SessionEvents {

  def init() = initSessionEvents()
  
  def dispose() = disposeSessionEvents()
  
  override protected def sessionEvents(session: Session): List[Event_Bus[_]] = sessionEvents0(session)
  
  /** a bit of a hack to get the types working correctly */
  protected def sessionEvents0(session: Session): List[_ <: Event_Bus[T]]
  
}
