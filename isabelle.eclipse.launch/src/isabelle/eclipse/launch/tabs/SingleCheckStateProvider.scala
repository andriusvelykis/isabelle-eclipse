package isabelle.eclipse.launch.tabs

import org.eclipse.jface.viewers.{
  CheckStateChangedEvent,
  ICheckStateListener,
  ICheckStateProvider,
  ICheckable,
  StructuredViewer
}


/**
 * A check state provider that allows only a single element to be selected.
 * 
 * Furthermore, provides element check state caching capabilities, e.g. if the tree is used with
 * filters. Even if the checked element is filtered, the selected state is still displayed
 * correctly when filter is cleared.
 * 
 * A viewer that is used with the provider must be initialized using #initViewer(V)
 * 
 * @tparam V  The actual viewer type to use with the provider (supports both CheckboxTableViewer
 *            and CheckboxTreeViewer).
 * 
 * @author Andrius Velykis
 */
class SingleCheckStateProvider[V <: StructuredViewer with ICheckable]
    extends ICheckStateProvider {

  private var _viewer: V = _
  private var _checked: Option[AnyRef] = None

  def viewer: V = _viewer

  def checked: Option[AnyRef] = _checked

  def checked_=(elem: Option[AnyRef]) = {
    val previousChecked = _checked
    _checked = elem

    // signal update (previous changed and new changed)
    val changed = List[AnyRef]() ++ previousChecked ++ _checked
    viewer.update(changed.toArray, null)

    // also reveal in viewer
    _checked foreach viewer.reveal
  }

  override def isChecked(element: Any): Boolean = checked.isDefined && element == checked.get

  // never grayed
  override def isGrayed(element: Any): Boolean = false

  
  /**
   * Initializes the viewer to use with this provider. Viewer check state is synchronized with the
   * provider.
   */
  def initViewer(viewer: V) {

    _viewer = viewer

    // listen for checks (e.g. using mouse) on the viewer
    viewer.addCheckStateListener(new ICheckStateListener {
      override def checkStateChanged(event: CheckStateChangedEvent) {
        checked = if (event.getChecked) Some(event.getElement) else None
      }
    })
  }

}
