package isabelle.eclipse.ui.util

import java.lang.reflect.InvocationTargetException

import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.{Event, Listener, Tree}


/**
 * Some support for setting tree row height.
 * 
 * SWT has little support for this functionality. As a workaround, it is possible to change row
 * height on Windows and Mac via reflection. Unfortunately, no such solution is available for
 * Linux.
 * 
 * The official way (without reflection) to change row height is via SWT MeasureItem listener.
 * This, however, works only for increasing row height. After increase, it can no longer be
 * decreased without recreating the widget (Note that the solution with Tree#clearAll does seem
 * to achieve the correct effect on Linux).
 * 
 * Note that this seems to be quite a hack - see the links below for tracking the blocking bugs.
 * 
 * @see http://bugs.eclipse.org/bugs/show_bug.cgi?id=154341
 * @see http://bugs.eclipse.org/bugs/show_bug.cgi?id=148039
 * 
 * @author Andrius Velykis
 */
trait TreeRowHeight {

  def tree: Tree
  
  private var height: Int = SWT.DEFAULT

  // Resize the row height using a MeasureItem listener.
  //
  // This is needed to increase row height on Linux. Unfortunately, row height can only be increased
  // and then it stays increased for the widget. This is a known bug in SWT.
  tree.addListener(SWT.MeasureItem, new Listener {
    override def handleEvent(event: Event) {
      if (height != SWT.DEFAULT) {
        event.height = height;
      }
    }
  })

  /**
   * Sets row height to the tree. All rows are set to the same height (restriction on Windows SWT).
   * 
   * Tries different methods of setting row height, e.g. via reflection and other.
   */
  def setItemHeight(height: Int) {
    this.height = height
    // for Windows and Mac, try setting item height via reflection
    val heightSet = setItemHeightReflection(height)
    if (!heightSet) {
      // if did not manage to set height via reflection (e.g. on Linux),
      // try triggering repaint
      
      /*
       * The solution with Tree#clearAll(true) below actually does seem to achieve the correct
       * effect on Linux (tested on Ubuntu 12.10, 32-bit, GTK 2.24.13-0ubuntu2, I think).
       * 
       * I have also tried various combinations of `tree.redraw()`, `tree.layout()`, `tree.pack()`,
       * `tree.getColumns foreach (_.pack)` and `tree.getParent.pack()`, etc. Neither of these did
       * achieve the correct result, so leaving tree.clearAll(true) as the solution for now.
       * 
       * The tree.clearAll(true) also does not force full redraw (i.e. where the whole tree blinks)
       * but resizes the rows nicely, at least during my testing.
       */
      tree.clearAll(true)
    }
  }
  
  /**
   * Sets tree row height via reflection
   * (private method `setItemHeight` is available on Windows and Mac).
   */
  private def setItemHeightReflection(height: Int): Boolean =
    try {

      val method = tree.getClass.getDeclaredMethod("setItemHeight", classOf[Int])
      val accessible = method.isAccessible
      method.setAccessible(true)
      method.invoke(tree, Integer.valueOf(height))
      method.setAccessible(accessible)
      true

    } catch {
      case e: NoSuchMethodException => false
      case e: SecurityException => false
      case e: IllegalArgumentException => false
      case e: IllegalAccessException => false
      case e: InvocationTargetException => false
    }

}
