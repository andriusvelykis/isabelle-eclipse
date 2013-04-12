package isabelle.eclipse.ui.editors

import org.eclipse.core.runtime.IAdaptable
import org.eclipse.jface.resource.ResourceManager
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.contentassist.{ContentAssistant, IContentAssistant}
import org.eclipse.jface.text.reconciler.{IReconciler, MonoReconciler}
import org.eclipse.jface.text.source.ISourceViewer
import org.eclipse.swt.widgets.Widget
import org.eclipse.ui.editors.text.EditorsUI
import org.eclipse.ui.texteditor.spelling.{SpellingReconcileStrategy, SpellingService}

import isabelle.eclipse.ui.annotations.TheoryViewerAnnotations
import java.{util => ju}


/** @author Andrius Velykis */
class IsabelleTheoryConfiguration(editor: TheoryEditor,
                                  resourceManager: ResourceManager,
                                  widget: => Option[Widget],
                                  annotations: => Option[TheoryViewerAnnotations])
    extends IsabelleTheoryViewerConfiguration(
      editor.isabelleModel map (_.session),
      editor.isabelleModel map (_.snapshot),
      Some(editor),
      resourceManager) {

  override def getContentAssistant(sourceViewer: ISourceViewer): IContentAssistant = {

    val ca = new ContentAssistant()
    ca.enableAutoActivation(true)
    ca.enableAutoInsert(true)
    
    ca.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer))
    
    val pr = new IsabelleContentAssistProcessor(
        editor.isabelleModel map (_.session) map (_.recent_syntax),
        resourceManager)
    ca.setContentAssistProcessor(pr, IDocument.DEFAULT_CONTENT_TYPE)
    
    // set the same content assistant on all partition types
    IsabellePartitions.contentTypes foreach (
        contentType => ca.setContentAssistProcessor(pr, contentType))
    
    ca.setInformationControlCreator(getInformationControlCreator(sourceViewer))
    ca
  }

  /** The hyperlink detector target ID, as defined in plugin.xml */
  val ISABELLE_THEORY_HYPERLINK_TARGET = "isabelle.eclipse.ui.theoryEditor"

  override def getHyperlinkDetectorTargets(sourceViewer: ISourceViewer): ju.Map[String, IAdaptable] = {

    val targets = super.getHyperlinkDetectorTargets(sourceViewer).asInstanceOf[ju.Map[String, IAdaptable]]

    // mark the editor as valid target for Isabelle Theory hyperlink detectors
    // (attaches the detector to the editor)
    targets.put(ISABELLE_THEORY_HYPERLINK_TARGET, editor)
    targets
  }

  override def getReconciler(sourceViewer: ISourceViewer): IReconciler = {

    // reimplement spelling service from the parent
    val spellingService = Option(preferenceStore).filter {
      _.getBoolean(SpellingService.PREFERENCE_SPELLING_ENABLED)
    } flatMap { prefs =>
      val spelling = EditorsUI.getSpellingService
      Option(spelling.getActiveSpellingEngineDescriptor(prefs)) match {
        case Some(_) => Some(spelling)
        case _ => None
      }
    }

    // add a reconciler that refreshes annotations
    // this is necessary to avoid missing events from Isabelle which do not remove annotations
    val annRefresh = new AnnotationRefreshReconcilingStrategy(widget, annotations)
    val spelling = spellingService map { spelling =>
      new SpellingReconcileStrategy(sourceViewer, spelling)
    } 


    val strategies = annRefresh :: spelling.toList

    val compositeStrategy = strategies match {
      case single :: Nil => single
      case multiple => new CompositeReconcilingStrategy(multiple)
    }

    val reconciler = new MonoReconciler(compositeStrategy, true)
    reconciler.setDelay(500)
    reconciler
  }

}
