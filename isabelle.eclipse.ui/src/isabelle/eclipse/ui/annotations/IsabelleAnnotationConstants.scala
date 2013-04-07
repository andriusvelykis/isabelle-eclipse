package isabelle.eclipse.ui.annotations

import org.eclipse.core.resources.IMarker

import isabelle.eclipse.core.text.IsabelleAnnotation._


/**
 * Defines the constants used to render Isabelle annotations/markers. Contains annotation/marker
 * types to instantiate and mapping from abstract annotation type (`IsabelleAnnotation`) to
 * concrete annotation/marker types.
 * 
 * @author Andrius Velykis
 */
object IsabelleAnnotationConstants {

  val MARKER_PROBLEM = "isabelle.eclipse.ui.markerProblem"
  val MARKER_LEGACY = "isabelle.eclipse.ui.markerLegacy"
  val MARKER_INFO = "isabelle.eclipse.ui.markerInfo"
  
  // default annotations for the problem/info markers
  val ANNOTATION_ERROR = "org.eclipse.ui.workbench.texteditor.error"
  val ANNOTATION_WARNING = "org.eclipse.ui.workbench.texteditor.warning"

  val ANNOTATION_LEGACY = "isabelle.eclipse.ui.annotation.legacy"
  val ANNOTATION_INFO = "isabelle.eclipse.ui.annotation.info"
  val ANNOTATION_TRACING = "isabelle.eclipse.ui.annotation.tracing"
  
  // TODO foreground colours, Isabelle_Markup.foreground? Or actually syntax colours?
  val ANNOTATION_BAD = "isabelle.eclipse.ui.annotation.markup.bad"
  val ANNOTATION_INTENSIFY = "isabelle.eclipse.ui.annotation.markup.intensify"
  val ANNOTATION_TOKEN = "isabelle.eclipse.ui.annotation.markup.token"
  
  val ANNOTATION_OUTDATED = "isabelle.eclipse.ui.annotation.cmd.outdated"
  val ANNOTATION_UNFINISHED = "isabelle.eclipse.ui.annotation.cmd.unfinished"
  // TODO use Isabelle's colors? At least as preference defaults?
  val ANNOTATION_UNPROCESSED = "isabelle.eclipse.ui.annotation.cmd.unprocessed"
//  val ANNOTATION_FAILED = "isabelle.eclipse.ui.annotation.cmd.failed"
//  val ANNOTATION_FINISHED = "isabelle.eclipse.ui.annotation.cmd.finished"


  val annotationTypes = Map(
    MARKUP_BAD -> ANNOTATION_BAD,
    MARKUP_INTENSIFY -> ANNOTATION_INTENSIFY,
    MARKUP_TOKEN_RANGE -> ANNOTATION_TOKEN,
    MESSAGE_ERROR -> ANNOTATION_ERROR,
    MESSAGE_LEGACY -> ANNOTATION_LEGACY,
    MESSAGE_WARNING -> ANNOTATION_WARNING,
    MESSAGE_WRITELN -> ANNOTATION_INFO,
    MESSAGE_TRACING -> ANNOTATION_TRACING,
    STATUS_OUTDATED -> ANNOTATION_OUTDATED,
    STATUS_UNFINISHED -> ANNOTATION_UNFINISHED,
    STATUS_UNPROCESSED -> ANNOTATION_UNPROCESSED
    )


  val markerTypes = Map(
    MESSAGE_ERROR -> MarkerInfo(MARKER_PROBLEM, IMarker.SEVERITY_ERROR),
    MESSAGE_WARNING -> MarkerInfo(MARKER_PROBLEM, IMarker.SEVERITY_WARNING),
    MESSAGE_LEGACY -> MarkerInfo(MARKER_LEGACY, IMarker.SEVERITY_WARNING),
    MESSAGE_WRITELN -> MarkerInfo(MARKER_INFO, IMarker.SEVERITY_INFO)
    )


  case class MarkerInfo(key: String, severity: Int)

}
