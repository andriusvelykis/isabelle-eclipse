package isabelle.eclipse.ui.annotations

import java.util.{Map => JavaMap}

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq

import org.eclipse.core.resources.{IMarker, IResource, IWorkspace, IWorkspaceRunnable}
import org.eclipse.core.runtime.{CoreException, IProgressMonitor}
import org.eclipse.jface.text.BadLocationException
import org.eclipse.jface.text.source.IAnnotationModelExtension

import isabelle.Text.Range
import isabelle.eclipse.core.text.{AnnotationInfo, IsabelleAnnotation}
import isabelle.eclipse.ui.annotations.IsabelleAnnotationConstants.MarkerInfo
import isabelle.eclipse.ui.internal.IsabelleUIPlugin.{error, log}


/**
 * An extension of Isabelle annotation support that creates resource Markers for certain annotation
 * types (e.g. error, warning, etc).
 * 
 * Needs an associated resource to set the markers on.
 * 
 * @author Andrius Velykis
 */
trait IsabelleMarkerAnnotations extends IsabelleAnnotations {

  def markerResource: IResource

  // remove marker types from annotation types to avoid duplication
  override val annotationTypes: Map[IsabelleAnnotation, String] =
    IsabelleAnnotationConstants.annotationTypes -- IsabelleAnnotationConstants.markerTypes.keySet

  def markerTypes: Map[IsabelleAnnotation, MarkerInfo] = IsabelleAnnotationConstants.markerTypes

  private var existingMarkers: Map[AnnotationInfo, MarkerRef] = Map()

  override protected def doReplaceAnnotations(docRange: Range,
                                              newAnnDefs: Seq[AnnotationInfo],
                                              deleteAnnDefs: Seq[AnnotationInfo]) {
    super.doReplaceAnnotations(docRange, newAnnDefs, deleteAnnDefs)

    val addMarkers = createMarkers(docRange, newAnnDefs)

    val currentMarkers = existingMarkers
    val deleteMarkers = (deleteAnnDefs map currentMarkers.get).flatten

    val afterMarkers = (currentMarkers -- deleteAnnDefs) ++ addMarkers
    this.existingMarkers = afterMarkers

    // update the markers in the workspace
    setMarkers(markerResource, deleteMarkers, addMarkers.values)
  }

  private def createMarkers(docRange: Range,
                            newAnnDefs: Seq[AnnotationInfo]): 
      (Map[AnnotationInfo, MarkerRef]) = {

    val markers = newAnnDefs.map { annDef => createMarker(docRange, annDef) map ((annDef, _)) }

    val validMarkers = markers.flatten
    validMarkers.toMap
  }


  private def createMarker(docRange: Range, annDef: AnnotationInfo): Option[MarkerRef] =
    // avoid creating annotation if its type is not available
    markerTypes.get(annDef.annType).map { markerInfo =>
      new MarkerRef(markerInfo.key, markerAttributes(docRange, annDef, markerInfo))
    }

  private def markerAttributes(docRange: Range,
                               annDef: AnnotationInfo,
                               markerInfo: MarkerInfo): JavaMap[String, AnyRef] = {

    // restrict the range to avoid exceeding the document range
    // (do not ignore marker if the range is invalid (e.g. outside the max range)
    // but better display it at (0, 0)
    val range = docRange.try_restrict(annDef.range) getOrElse Range(0, 0)

    val markerAttrs = Map[String, AnyRef](
      IMarker.SEVERITY -> (markerInfo.severity: Integer),
      IMarker.CHAR_START -> (range.start: Integer),
      IMarker.CHAR_END -> (range.stop: Integer),
      IMarker.MESSAGE -> annDef.message.orNull
      )

    val lineNumAttrs: Map[String, AnyRef] = try {
      // lines are 1-based
      val line = document.getLineOfOffset(range.start) + 1
      Map(
        IMarker.LOCATION -> ("line " + line),
        IMarker.LINE_NUMBER -> (line: Integer)
        )

    } catch {
      case ex: BadLocationException => Map() // ignore
    }

    val allAttrs = markerAttrs ++ lineNumAttrs
    allAttrs.asJava
  }

  private def setMarkers(resource: IResource,
                         deleteMarkers: Seq[MarkerRef],
                         addMarkers: Iterable[MarkerRef]) = withWorkspaceUpdates(resource) {

    // add new markers in the workspace runnable to control refresh
    
    // first delete existing markers, then add new ones
    deleteMarkers.foreach { _.delete() }

    // avoid creating already deleted markers (e.g. in concurrent case)
    val add = addMarkers.iterator.filterNot { _.deleted }
    add foreach { markerRef =>
      val marker = resource.createMarker(markerRef.key)
      markerRef.marker = Some(marker)
      marker.setAttributes(markerRef.attrs)
    }
  }


  private def withWorkspaceUpdates(resource: IResource)(f: => Any) {
    val runnable = new IWorkspaceRunnable {
      @throws[CoreException]
      override def run(monitor: IProgressMonitor) {
        f
      }
    }

    try {
      resource.getWorkspace.run(runnable, resource, IWorkspace.AVOID_UPDATE, null)
    } catch {
      case ce: CoreException => log(error(Some(ce)))
    }
  }

  private class MarkerRef(val key: String, val attrs: JavaMap[String, AnyRef]) {
    var deleted = false
    var marker: Option[IMarker] = None
    
    def delete() {
      deleted = true
      marker.foreach (_.delete())
    }
  }

}
