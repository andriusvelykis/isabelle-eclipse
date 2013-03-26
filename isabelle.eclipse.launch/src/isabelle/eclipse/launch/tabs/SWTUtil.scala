package isabelle.eclipse.launch.tabs

import org.eclipse.swt.widgets.Widget


/**
 * Various utility methods related to SWT components and UI interaction.
 * 
 * (Copied from [[isabelle.eclipse.ui.util.SWTUtil]] to avoid adding dependency)
 * 
 * @author Andrius Velykis
 */
object SWTUtil {

  /**
   * Asynchronously runs `f` on the UI thread.
   * 
   * Executes only if the given widget is available and is not disposed before and at the start
   * of execution.
   */
  def asyncUnlessDisposed(widget: Option[Widget])(f: => Unit) = widget foreach { w =>
    if (!w.isDisposed) {
      w.getDisplay asyncExec new Runnable {
        // check again that widget is not disposed, only then execute.
        // This is necessary because the widget may had been disposed before the turn for this
        // execution came
        override def run() = if (!w.isDisposed) { f }
      }
    }
  }

}
