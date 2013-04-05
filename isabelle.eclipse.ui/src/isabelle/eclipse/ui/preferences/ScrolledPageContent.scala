package isabelle.eclipse.ui.preferences

import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.{Composite, Control}
import org.eclipse.ui.forms.FormColors
import org.eclipse.ui.forms.widgets.{FormToolkit, SharedScrolledComposite}


/**
 * Adapted from org.eclipse.jdt.internal.ui.preferences.ScrolledPageContent
 *
 * @author Andrius Velykis
 */
class ScrolledPageContent(parent: Composite, style: Int)
    extends SharedScrolledComposite(parent, style) {

  def this(parent: Composite) = this(parent, SWT.V_SCROLL | SWT.H_SCROLL)

  setFont(parent.getFont)

  private val toolkit: FormToolkit = {
    val colors = new FormColors(parent.getDisplay)
    colors.setBackground(null)
    colors.setForeground(null)

    new FormToolkit(colors)
  }

  setExpandHorizontal(true)
  setExpandVertical(true)

  {
    val body = new Composite(this, SWT.NONE)
    body.setFont(parent.getFont)
    setContent(body)
  }

  /**
   * @see org.eclipse.swt.widgets.Widget#dispose()
   */
  override def dispose() {
    toolkit.dispose()
    super.dispose()
  }

  def adaptChild(childControl: Control) = toolkit.adapt(childControl, true, true)

  def getBody: Composite = getContent.asInstanceOf[Composite]

}