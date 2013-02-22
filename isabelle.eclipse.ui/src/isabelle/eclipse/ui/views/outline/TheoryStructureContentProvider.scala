package isabelle.eclipse.ui.views.outline

import org.eclipse.jface.viewers.{ITreeContentProvider, Viewer}


/**
 * A tree content provider for Thy_Syntax.Structure entry elements.
 * 
 * Displays block-level elements and heading-level commands in the tree.
 * 
 * @author Andrius Velykis
 * @see TheoryStructureEntry
 */
/* Adapted from Isabelle_Sidekick_Structure */
class TheoryStructureContentProvider extends ITreeContentProvider {

  override def getElements(parentElement: AnyRef): Array[AnyRef] =
    getChildren(parentElement)
  
  override def getChildren(parentElement: AnyRef): Array[AnyRef] = parentElement match {
    case entry: TheoryStructureEntry => entry.children.toArray
    case _ => Array()
  }

  override def getParent(element: AnyRef): AnyRef = element match {
    case entry: TheoryStructureEntry => entry.parent.orNull
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
