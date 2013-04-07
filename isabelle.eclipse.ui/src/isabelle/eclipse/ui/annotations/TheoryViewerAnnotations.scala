package isabelle.eclipse.ui.annotations

import isabelle.Document.Snapshot
import isabelle.Text.Range
import isabelle.eclipse.core.text.AnnotationFactory


/**
 * Updater for Isabelle theory viewer annotations: creates annotations from the given
 * document snapshot.
 *
 * @author Andrius Velykis
 */
class TheoryViewerAnnotations(snapshot: => Option[Snapshot],
                              anns: => Option[IsabelleAnnotations]) {

  /**
   * Must be called from the UI thread, otherwise getting ConcurrentModificationException
   * on the document positions.
   * (e.g. when setting the annotations and repainting them at the same time)
   */
  def updateAnnotations(changedRanges: Option[List[Range]] = None) = (snapshot, anns) match {

    case (Some(s), Some(annotations)) => {

      // Only create new annotations if the Isabelle document snapshot is available
      // Note that old persistent markers are not deleted if present then

      // if changed ranges not defined, it means "all" - create document length range
      val ranges = changedRanges getOrElse List(annotations.documentRange)

      // generate annotations for the changed range and replace in the model
      if (!ranges.isEmpty) {
        val annDefs = AnnotationFactory.createAnnotations(s, ranges)
        annotations.replaceAnnotationsRange(annDefs, changedRanges)
      }
    }

    case _ => // ignore
  }

}
