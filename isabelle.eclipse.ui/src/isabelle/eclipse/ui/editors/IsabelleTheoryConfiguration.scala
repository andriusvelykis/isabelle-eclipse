package isabelle.eclipse.ui.editors

import org.eclipse.jface.resource.ResourceManager
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.contentassist.{ContentAssistant, IContentAssistant}
import org.eclipse.jface.text.source.ISourceViewer


/** @author Andrius Velykis */
class IsabelleTheoryConfiguration(editor: TheoryEditor, resourceManager: ResourceManager)
  extends IsabelleTheoryViewerConfiguration(editor, resourceManager) {

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

}
