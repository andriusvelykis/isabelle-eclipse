package isabelle.eclipse.ui.util

import org.eclipse.swt.events.{DisposeEvent, DisposeListener}
import org.eclipse.swt.graphics.{Font, FontMetrics, GC}
import org.eclipse.swt.widgets.{Control, Widget}


/**
 * Various utility methods related to SWT components and UI interaction.
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


  implicit class Disposable(control: Widget) {
    def onDispose(f: => Unit) {

      control.addDisposeListener(new DisposeListener {
        override def widgetDisposed(e: DisposeEvent) {
          f
          control.removeDisposeListener(this)
        }
      })

    }
  }


  /**
   * Get the metrics for the given font.
   * 
   * Needs to be called from the UI thread.
   */
  def initializeFontMetrics(control: Control, font: Font): FontMetrics = {
    val gc = new GC(control)
    gc.setFont(font)
    val fontMetrics = gc.getFontMetrics()
    gc.dispose()
    fontMetrics
  }

}
