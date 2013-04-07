package isabelle.eclipse.ui.editors

import scala.collection.immutable.Seq

import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.jface.text.{IDocument, IRegion}
import org.eclipse.jface.text.reconciler.{
  DirtyRegion,
  IReconcilingStrategy,
  IReconcilingStrategyExtension
}


/**
 * A reconciling strategy consisting of a sequence of internal reconciling strategies.
 * By default, all requests are passed on to the contained strategies.
 * 
 * @author Andrius Velykis
 */
class CompositeReconcilingStrategy(val strategies: Seq[IReconcilingStrategy])
    extends IReconcilingStrategy with IReconcilingStrategyExtension {

  override def setDocument(document: IDocument) =
    strategies.foreach { _.setDocument(document) }

  override def reconcile(dirtyRegion: DirtyRegion, subRegion: IRegion) =
    strategies.foreach { _.reconcile(dirtyRegion, subRegion) }

  override def reconcile(partition: IRegion) =
    strategies.foreach { _.reconcile(partition) }

  override def setProgressMonitor(monitor: IProgressMonitor) =
    foreachExtStrategy { _.setProgressMonitor(monitor) }

  override def initialReconcile() =
    foreachExtStrategy { _.initialReconcile() }


  private def foreachExtStrategy(f: IReconcilingStrategyExtension => Unit) =
    strategies.foreach {
      _ match {
        case s: IReconcilingStrategyExtension => f(s)
        case _ =>
      }
    }

}
