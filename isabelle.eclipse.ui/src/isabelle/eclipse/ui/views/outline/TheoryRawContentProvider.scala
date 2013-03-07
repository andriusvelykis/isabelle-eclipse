package isabelle.eclipse.ui.views.outline

import org.eclipse.jface.viewers.{ITreeContentProvider, Viewer}


/**
 * A tree content provider for raw XML markup structure of an Isabelle theory file.
 * 
 * @author Andrius Velykis
 * @see TheoryRawEntry
 */
/* Adapted from Isabelle_Sidekick_Raw */
class TheoryRawContentProvider extends ITreeContentProvider {

  override def getElements(parentElement: AnyRef): Array[AnyRef] = parentElement match {
    case list: TraversableOnce[_] => list.asInstanceOf[TraversableOnce[AnyRef]].toArray
    case _ => getChildren(parentElement)
  }
  
  override def getChildren(parentElement: AnyRef): Array[AnyRef] = parentElement match {
    case entry: TheoryRawEntry => entry.children.toArray
    case _ => Array()
  }

  override def getParent(element: AnyRef): AnyRef = element match {
    case entry: TheoryRawEntry => entry.parent.orNull
    case _ => null
  }

  override def hasChildren(element: AnyRef): Boolean = !getChildren(element).isEmpty
  
  
  /**
   * This implementation does nothing
   */
  override def inputChanged(viewer: Viewer, oldInput: AnyRef, newInput: AnyRef) {
    // do nothing
  }
  
  /**
   * This implementation does nothing
   */
  override def dispose() {
    // do nothing
  }
  
}
