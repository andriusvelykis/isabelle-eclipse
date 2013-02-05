package isabelle.eclipse.launch.tabs

import org.eclipse.swt.accessibility.{AccessibleAdapter, AccessibleEvent}
import org.eclipse.swt.widgets.Control


/**
 * Utilities for UI accessibility.
 * 
 * @author Andrius Velykis
 */
object AccessibleUtil {

  /*
   * Fix for Bug 60163 Accessibility: New Builder Dialog missing object info for textInput controls
   */
  def addControlAccessibleListener(control: Control, controlName: String) {
    //strip mnemonic (&)
    val stripped = controlName.replace("&", "")
    control.getAccessible.addAccessibleListener(new ControlAccessibleListener(stripped))
  }
  
  private class ControlAccessibleListener(controlName: String) extends AccessibleAdapter {
    override def getName(e: AccessibleEvent) {
      e.result = controlName
    }
  }
  
}
