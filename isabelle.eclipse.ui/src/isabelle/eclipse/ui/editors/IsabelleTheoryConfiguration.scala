package isabelle.eclipse.ui.editors

import java.util.Map

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

class IsabelleTheoryConfiguration(val editor: TheoryEditor, val colorManager: ColorManager)
  extends TextSourceViewerConfiguration(EditorsUI.getPreferenceStore()) {

  /** The hyperlink detector target ID, as defined in plugin.xml */
  val ISABELLE_THEORY_HYPERLINK_TARGET = "isabelle.eclipse.ui.theoryEditor"
  
  val isabelleScanner = new IsabelleTokenScanner(editor) {

    override def createToken(tokenInfo: isabelle.Token) =
      new Token(new TextAttribute(colorManager.getColor(
        getTokenColor(tokenInfo))))
  }

  def getTokenColor(tokenInfo: isabelle.Token): RGB = {

    import isabelle.Token.Kind._

    tokenInfo.kind match {
      case COMMENT => IXMLColorConstants.XML_COMMENT
      case COMMAND | KEYWORD => IXMLColorConstants.TAG
      case ALT_STRING | FLOAT | NAT | STRING => IXMLColorConstants.STRING
      case _ => IXMLColorConstants.DEFAULT
    }
  }

  override def getContentAssistant(sourceViewer: ISourceViewer): IContentAssistant = {

    val ca = new ContentAssistant()
    val pr = new IsabelleContentAssistProcessor(editor)
    ca.setContentAssistProcessor(pr, IDocument.DEFAULT_CONTENT_TYPE)
    ca.setInformationControlCreator(getInformationControlCreator(sourceViewer))
    ca
  }

  override def getPresentationReconciler(sourceViewer: ISourceViewer): IPresentationReconciler = {
    val reconciler = super.getPresentationReconciler(sourceViewer).asInstanceOf[PresentationReconciler]

    val dr = new DefaultDamagerRepairer(isabelleScanner)

    reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE)
    reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE)
    reconciler
  }

  override def getHyperlinkDetectorTargets(sourceViewer: ISourceViewer): Map[String, IAdaptable] = {

    val targets = super.getHyperlinkDetectorTargets(sourceViewer).asInstanceOf[Map[String, IAdaptable]]

    // mark the editor as valid target for Isabelle Theory hyperlink detectors
    // (attaches the detector to the editor)
    targets.put(ISABELLE_THEORY_HYPERLINK_TARGET, editor);
    targets;
  }

}
