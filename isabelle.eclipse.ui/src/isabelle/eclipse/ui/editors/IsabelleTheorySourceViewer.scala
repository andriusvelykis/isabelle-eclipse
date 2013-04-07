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
import org.eclipse.jface.util.{IPropertyChangeListener, PropertyChangeEvent}
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
import isabelle.eclipse.ui.annotations.{
  IsabelleAnnotationConstants,
  IsabelleAnnotations,
  TheoryViewerAnnotations
}
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

  private val annotations = new TheoryViewerAnnotations(snapshot, annotationModel)

  def annotationModel: Option[IsabelleAnnotations] = Option(getAnnotationModel) match {
    case Some(isa: IsabelleAnnotations) => Some(isa)
    case _ => None
  }

  
  def fontKey = IsabelleUIPreferences.ISABELLE_FONT

  val preferenceListener = new IPropertyChangeListener {
    override def propertyChange(event: PropertyChangeEvent) = handlePreferenceStoreChanged(event)
  }
  configuration.preferenceStore.addPropertyChangeListener(preferenceListener)

  
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
    configuration.preferenceStore.removePropertyChangeListener(preferenceListener)
    resourceManager.dispose()
    decorationSupport.dispose()
  }
  
  private def handlePreferenceStoreChanged(event: PropertyChangeEvent) {
    // notify configuration to update syntax highlighting
    configuration.handlePropertyChangeEvent(event)
    
    // invalidate text presentation, otherwise the syntax highlighting does not get changed
    // TODO investigate a more precise refresh (not on every preference change)
    invalidateTextPresentation()

    // TODO support other preference changes (see TextEditor#handlePreferenceStoreChanged
    // and its parents) - issue #37
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
    val annotationTypes = IsabelleAnnotationConstants.annotationTypes.keySet
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
