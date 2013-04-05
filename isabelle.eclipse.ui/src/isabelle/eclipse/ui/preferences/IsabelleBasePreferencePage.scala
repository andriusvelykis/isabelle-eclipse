package isabelle.eclipse.ui.preferences

import org.eclipse.jface.preference.PreferencePage
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.{Composite, Control}
import org.eclipse.ui.{IWorkbench, IWorkbenchPreferencePage}

import isabelle.eclipse.ui.internal.IsabelleUIPlugin


/**
 * The page for Isabelle/Eclipse preferences root - empty at the moment.
 *
 * @author Andrius Velykis
 */
class IsabelleBasePreferencePage extends PreferencePage with IWorkbenchPreferencePage {

  setPreferenceStore(IsabelleUIPlugin.plugin.getPreferenceStore)

  override protected def createContents(parent: Composite): Control =
    new Composite(parent, SWT.NONE)

  /**
   * @see IWorkbenchPreferencePage
   */
  override def init(workbench: IWorkbench) {}

}