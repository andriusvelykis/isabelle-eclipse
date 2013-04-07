package isabelle.eclipse.ui.editors

import org.eclipse.jface.text.{IDocument, IRegion}
import org.eclipse.jface.text.reconciler.{DirtyRegion, IReconcilingStrategy}
import org.eclipse.swt.widgets.Widget

import isabelle.Text.Range
import isabelle.eclipse.ui.annotations.TheoryViewerAnnotations
import isabelle.eclipse.ui.util.SWTUtil


/**
 * A reconciling strategy that updates annotations for the dirty regions.
 *
 * Used to ensure full refresh of Isabelle elements.
 *
 * @author Andrius Velykis
 */
class AnnotationRefreshReconcilingStrategy(widget: => Option[Widget],
                                           annotations: => Option[TheoryViewerAnnotations])
  extends IReconcilingStrategy {

  override def setDocument(document: IDocument) {}

  override def reconcile(dirtyRegion: DirtyRegion, subRegion: IRegion) {
    reconcile(subRegion)
  }

  override def reconcile(partition: IRegion) = SWTUtil.asyncUnlessDisposed(widget) {
    annotations match {

      case Some(annotations) => {
        val range = Range(partition.getOffset, partition.getOffset + partition.getLength)
        annotations.updateAnnotations(Some(List(range)))
      }

      case _ =>
    }
  }

}