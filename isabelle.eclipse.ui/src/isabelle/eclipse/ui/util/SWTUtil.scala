package isabelle.eclipse.ui.util

import org.eclipse.swt.widgets.Display
import org.eclipse.ui.IWorkbenchPart


/**
 * Various utility methods related to SWT components and UI interaction.
 * 
 * @author Andrius Velykis
 */
object SWTUtil {

  /**
   * Asynchronously run `f` on the UI thread.
   */
  def asyncExec(display: Option[Display] = None)(f: => Unit) {
    val d = (display getOrElse Display.getDefault)

    if (!d.isDisposed) {
      d asyncExec new Runnable {
        override def run() { f }
      }
    }
  }

  
  /**
   * Asynchronously run `f` on the UI thread.
   */
  def runInUI(part: IWorkbenchPart)(f: => Unit) {

    val display = part.getSite.getWorkbenchWindow.getWorkbench.getDisplay
    asyncExec(Some(display))(f)
  }

}
