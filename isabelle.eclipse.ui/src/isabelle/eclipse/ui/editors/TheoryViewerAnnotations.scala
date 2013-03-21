package isabelle.eclipse.ui.editors

import scala.collection.JavaConverters._

import org.eclipse.core.resources.IResource
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.source.{IAnnotationModel, IAnnotationModelExtension}
import org.eclipse.swt.widgets.Display

import isabelle.Document.Snapshot
import isabelle.Text.Range
import isabelle.eclipse.core.text.{AnnotationFactory, AnnotationInfo}
import isabelle.eclipse.ui.internal.IsabelleUIPlugin.{error, log}


/**
 * Updater for Isabelle theory viewer annotations: creates annotations from the given
 * document snapshot.
 *
 * @author Andrius Velykis
 */
class TheoryViewerAnnotations(snapshot: => Option[Snapshot],
                              document: => IDocument,
                              annotationModel: => Option[IAnnotationModel],
                              markerResource: => Option[IResource] = None,
                              display: => Option[Display] = None) {

  /**
   * Must be called from the UI thread, otherwise getting ConcurrentModificationException
   * on the document positions.
   * (e.g. when setting the annotations and repainting them at the same time)
   */
  def updateAnnotations(changedRanges: Option[List[Range]] = None) = snapshot foreach { s =>

    // Only create new annotations if the Isabelle document snapshot is available
    // Note that old persistent markers are not deleted if present then

    // if changed ranges not defined, it means "all" - create document length range
    val ranges = changedRanges getOrElse List(Range(0, document.getLength))
    
    // generate annotations for the changed range and replace in the model
    if (!ranges.isEmpty) {
      val annotations = AnnotationFactory.createAnnotations(s, ranges)
      setAnnotations(annotations, ranges)
    }
  }


  private def setAnnotations(annotations: List[AnnotationInfo], changedRanges: List[Range]) =
    withAnnotationModel { annModel =>

      val annUpdater = new AnnotationUpdater(annModel, document, markerResource)
      annUpdater.updateAnnotations(changedRanges.asJava, annotations.asJava);
    }


  private def withAnnotationModel(f: IAnnotationModelExtension => Unit) {

    annotationModel match {
      case Some(modern: IAnnotationModelExtension) => f(modern)
      case Some(old) => 
        log(error(msg = Some("Obsolete annotation model is used: " + old.getClass)))
      case _ => {}
    }
  }

}
