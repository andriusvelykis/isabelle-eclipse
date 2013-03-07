package isabelle.eclipse.ui.editors

import scala.collection.JavaConverters._

import org.eclipse.jface.resource.{JFaceResources, LocalResourceManager, ResourceManager}
import org.eclipse.jface.text.source.{
  Annotation,
  AnnotationRulerColumn,
  CompositeRuler,
  IAnnotationAccess,
  IOverviewRuler,
  ISharedTextColors,
  IVerticalRuler,
  OverviewRuler,
  SourceViewer
}
import org.eclipse.swt.widgets.Composite
import org.eclipse.ui.texteditor.{
  AnnotationPreference,
  DefaultMarkerAnnotationAccess,
  MarkerAnnotationPreferences,
  SourceViewerDecorationSupport
}
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants

import isabelle.Document.Snapshot
import isabelle.Session
import isabelle.eclipse.ui.preferences.IsabelleUIPreferences
import isabelle.eclipse.ui.util.SWTUtil.Disposable


/**
 * An extension of SourceViewer with Isabelle theory specific customisations.
 * 
 * Most adapted from AbstractDecoratedTextEditor and AbstractTextEditor.
 * 
 * @author Andrius Velykis
 */
class IsabelleTheorySourceViewer private (
  parent: Composite,
  session: => Option[Session],
  snapshot: => Option[Snapshot],
  style: Int,
  resourceManager: ResourceManager,
  sharedColors: ISharedTextColors,
  annotationAccess: IAnnotationAccess,
  annotationPrefs: MarkerAnnotationPreferences,
  verticalRuler: IVerticalRuler,
  overviewRuler: IOverviewRuler,
  targetEditor: => Option[TheoryEditor])
    extends SourceViewer(parent, verticalRuler, overviewRuler, true, style) with FontUpdates {

  // cleanup on control dispose
  getControl onDispose disposeViewer()


  private val configuration = new IsabelleTheoryViewerConfiguration(
      session, snapshot, targetEditor, resourceManager)
  configure(configuration)
  
  private val decorationSupport = configureDecorationSupport()

  private val annotations = new TheoryViewerAnnotations(
    snapshot,
    getDocument,
    Option(getAnnotationModel),
    display = Some(getControl.getDisplay))

  
  def fontKey = IsabelleUIPreferences.ISABELLE_FONT

  
  private def configureDecorationSupport(): SourceViewerDecorationSupport = {

    import IsabelleTheorySourceViewer.AnnotationPrefsScala
    import AbstractDecoratedTextEditorPreferenceConstants._

    val decorationSupport = new SourceViewerDecorationSupport(
      this, overviewRuler, annotationAccess, sharedColors)

    annotationPrefs.iterator foreach (pref => decorationSupport.setAnnotationPreference(pref))

    decorationSupport.setCursorLinePainterPreferenceKeys(
      EDITOR_CURRENT_LINE, EDITOR_CURRENT_LINE_COLOR)
    decorationSupport.setMarginPainterPreferenceKeys(
      EDITOR_PRINT_MARGIN, EDITOR_PRINT_MARGIN_COLOR, EDITOR_PRINT_MARGIN_COLUMN)
    decorationSupport.setSymbolicFontName(fontKey)

    decorationSupport.install(configuration.preferenceStore)

    decorationSupport
  }


  def updateAnnotations() = annotations.updateAnnotations()


  private def disposeViewer() {
    resourceManager.dispose()
    decorationSupport.dispose()
  }
}


/**
 * @author Andrius Velykis
 */
object IsabelleTheorySourceViewer {
  
  private implicit class AnnotationPrefsScala(prefs: MarkerAnnotationPreferences) {
    def iterator: Iterator[AnnotationPreference] = prefs.getAnnotationPreferences.iterator.
      asInstanceOf[java.util.Iterator[AnnotationPreference]].asScala
  }

  private def createViewerAnnotations(sharedColors: ISharedTextColors)
      : (IAnnotationAccess, MarkerAnnotationPreferences, IOverviewRuler) = {

    val annotationAccess = new DefaultMarkerAnnotationAccess
    val annotationPreferences = new MarkerAnnotationPreferences

    val verticalRulerWidth = 12
    val ruler = new OverviewRuler(annotationAccess, verticalRulerWidth, sharedColors)

    annotationPreferences.iterator foreach { pref =>
      if (pref.contributesToHeader) {
        ruler.addHeaderAnnotationType(pref.getAnnotationType)
      }
    }

    (annotationAccess, annotationPreferences, ruler)
  }


  private def createVerticalRuler(annotationAccess: IAnnotationAccess): IVerticalRuler = {
    val ruler = new CompositeRuler
    
    val verticalRulerWidth = 12
    val anotationRulerColumn = new AnnotationRulerColumn(verticalRulerWidth, annotationAccess)
    
    // add types explicitly
    // TODO load from preferences?
    anotationRulerColumn.addAnnotationType(Annotation.TYPE_UNKNOWN)
    val annotationTypes = IsabelleAnnotationConstants.ANNOTATION_TYPES.keySet.asScala
    annotationTypes foreach anotationRulerColumn.addAnnotationType
    
    // temporarily remove Info annotations
    // TODO review this
    anotationRulerColumn.removeAnnotationType("isabelle.eclipse.ui.annotation.info")
    
    ruler.addDecorator(0, anotationRulerColumn)
    
    ruler
  }


  def apply(parent: Composite,
            session: => Option[Session],
            snapshot: => Option[Snapshot],
            targetEditor: => Option[TheoryEditor],
            style: Int): IsabelleTheorySourceViewer = {

    val resourceManager = new LocalResourceManager(JFaceResources.getResources)
    val sharedColors = new ManagedTextColors(resourceManager)

    val (annotationAccess, annotationPrefs, overviewRuler) = createViewerAnnotations(sharedColors)
    val verticalRuler = createVerticalRuler(annotationAccess)

    new IsabelleTheorySourceViewer(parent,
      session,
      snapshot,
      style,
      resourceManager,
      sharedColors,
      annotationAccess,
      annotationPrefs,
      verticalRuler,
      overviewRuler,
      targetEditor)
  }
  
}
