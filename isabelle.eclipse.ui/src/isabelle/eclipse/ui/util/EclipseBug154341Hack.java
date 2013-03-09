package isabelle.eclipse.ui.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Tree;

/**
 * Hack to work around Eclipse Bug 154341.
 * See <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=154341">https://bugs.eclipse.org/bugs/show_bug.cgi?id=154341</a>
 */
public class EclipseBug154341Hack {
  
  /*
  How it works:
  
  We use the workaround described by Chris Williams:
  
  Setting the font on the tree doesn't shrink it at all.
  Returning a smaller height for all rows via a MeasureItem listener doesn't either.
  
  I'd prefer not to have to go down that low level to do custom drawing just to set the row height again.
  I have platform specific code in reflection for mac and windows to handle this, but it's pretty ugly.
  When font size changes on windows I call setItemHeight on the tree through reflection.
  For Mac Cocoa I call setRowHeight on the view field on the Tree control via reflection.
  */
  
  /**
   * Sets the item height of the given {@link TreeViewer} to the specified value.
   * @param viewer the tree viewer to set the item height
   * @param height the height to set, must be > 0
   */
  public static void setItemHeight(TreeViewer viewer, int height) {
    setItemHeight(viewer.getTree(), height);
  }
  
  /**
   * Sets the item height of the given {@link Tree} to the specified value.
   * @param tree the tree to set the item height
   * @param height the height to set, must be > 0
   */
  public static void setItemHeight(Tree tree, int height) {
    try {
      Method method = null;
      
      Method[] methods = tree.getClass().getDeclaredMethods();
      method = findMethod(methods, "setItemHeight", 1); //$NON-NLS-1$
      if (method != null) {
        boolean accessible = method.isAccessible();
        method.setAccessible(true);
        method.invoke(tree, Integer.valueOf(height));
        method.setAccessible(accessible);
      }
    } catch (SecurityException e) {
      // ignore
    } catch (IllegalArgumentException e) {
      // ignore
    } catch (IllegalAccessException e) {
      // ignore
    } catch (InvocationTargetException e) {
      // ignore
    }
  }
  
  /**
   * Finds the method with the given name and parameter count from the specified methods.
   * @param methods the methods to search through
   * @param name the name of the method to find
   * @param parameterCount the count of parameters of the method to find
   * @return the method or <code>null</code> if not found
   */
  private static Method findMethod(Method[] methods, String name, int parameterCount) {
    for (Method method : methods) {
      if (method.getName().equals(name) && method.getParameterTypes().length == parameterCount) {
        return method;
      }
    }
    return null;
  }

}
