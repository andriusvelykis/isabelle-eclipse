package isabelle.eclipse.launch.tabs

import org.eclipse.jface.viewers.{ArrayContentProvider, ITreeContentProvider}

/**
 * An extension of ArrayContentProvider to support trees.
 * 
 * All arrays/collections are expanded, others are kept as leaves.
 * 
 * @author Andrius Velykis
 */
class ArrayTreeContentProvider extends ArrayContentProvider with ITreeContentProvider {

  override def getChildren(parentElement: AnyRef): Array[AnyRef] =
    getElements(parentElement)

  override def getParent(element: AnyRef): AnyRef = null

  override def hasChildren(element: AnyRef): Boolean = !getChildren(element).isEmpty

}
