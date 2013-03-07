package isabelle.eclipse.ui.editors

import org.eclipse.core.runtime.IAdaptable
import org.eclipse.jface.resource.ResourceManager
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.contentassist.{ContentAssistant, IContentAssistant}
import org.eclipse.jface.text.source.ISourceViewer

import java.{util => ju}


/** @author Andrius Velykis */
class IsabelleTheoryConfiguration(editor: TheoryEditor, resourceManager: ResourceManager)
    extends IsabelleTheoryViewerConfiguration(
      editor.isabelleModel map (_.session),
      editor.isabelleModel map (_.snapshot),
      Some(editor),
      resourceManager) {

  override def getContentAssistant(sourceViewer: ISourceViewer): IContentAssistant = {

    val ca = new ContentAssistant()
    ca.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer))
    
    val pr = new IsabelleContentAssistProcessor(editor, resourceManager)
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

}
