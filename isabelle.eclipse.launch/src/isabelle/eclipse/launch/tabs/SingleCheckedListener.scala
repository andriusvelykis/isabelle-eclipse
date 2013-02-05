package isabelle.eclipse.launch.tabs

import org.eclipse.jface.viewers.{CheckStateChangedEvent, CheckboxTableViewer, ICheckStateListener}

/**
 * A listener for CheckboxTableViewer that ensures only a single element is selected.
 * 
 * Whenever a new element is selected, the other ones are deselected.
 * 
 * @author Andrius Velykis
 */
class SingleCheckedListener(viewer: CheckboxTableViewer)
    extends ICheckStateListener {

  override def checkStateChanged(event: CheckStateChangedEvent) {
    if (event.getChecked) {
      viewer.setCheckedElements(Array(event.getElement()))
    }
  }

}
