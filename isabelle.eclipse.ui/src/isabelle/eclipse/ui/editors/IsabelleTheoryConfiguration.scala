package isabelle.eclipse.ui.editors

import java.util.Map
import org.eclipse.core.runtime.IAdaptable
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.TextAttribute
import org.eclipse.jface.text.contentassist.ContentAssistant
import org.eclipse.jface.text.contentassist.IContentAssistProcessor
import org.eclipse.jface.text.contentassist.IContentAssistant
import org.eclipse.jface.text.presentation.IPresentationReconciler
import org.eclipse.jface.text.presentation.PresentationReconciler
import org.eclipse.jface.text.rules.DefaultDamagerRepairer
import org.eclipse.jface.text.rules.IToken
import org.eclipse.jface.text.rules.ITokenScanner
import org.eclipse.jface.text.rules.Token
import org.eclipse.jface.text.source.ISharedTextColors
import org.eclipse.jface.text.source.ISourceViewer
import org.eclipse.swt.graphics.RGB
import org.eclipse.ui.editors.text.EditorsUI
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration
import org.eclipse.ui.texteditor.ChainedPreferenceStore

import isabelle.Outer_Syntax
import isabelle.eclipse.ui.IsabelleUIPlugin
import isabelle.eclipse.ui.text.AbstractIsabelleScanner
import isabelle.eclipse.ui.text.IsabelleMarkupScanner
import isabelle.eclipse.ui.text.IsabelleTokenScanner
import isabelle.eclipse.ui.text.ChainedTokenScanner
import isabelle.eclipse.ui.text.SingleTokenScanner
import isabelle.eclipse.ui.preferences.IsabellePartitionToSyntaxClass
import isabelle.eclipse.ui.preferences.IsabelleMarkupToSyntaxClass
import isabelle.eclipse.ui.preferences.IsabelleTokenToSyntaxClass


/** @author Andrius Velykis */
class IsabelleTheoryConfiguration(val editor: TheoryEditor, val colorManager: ISharedTextColors)
  extends TextSourceViewerConfiguration(new ChainedPreferenceStore(Array(
      // chain the preference store to get default editor preference values as well as Isabelle-specific
      IsabelleUIPlugin.getPreferences(),
      EditorsUI.getPreferenceStore()))) {

  /** The hyperlink detector target ID, as defined in plugin.xml */
  val ISABELLE_THEORY_HYPERLINK_TARGET = "isabelle.eclipse.ui.theoryEditor"
  
  override def getConfiguredDocumentPartitioning(sourceViewer: ISourceViewer) = 
    IsabellePartitions.ISABELLE_PARTITIONING
    
  override def getContentAssistant(sourceViewer: ISourceViewer): IContentAssistant = {

    val ca = new ContentAssistant()
    ca.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer))
    
    val pr = new IsabelleContentAssistProcessor(editor)
    ca.setContentAssistProcessor(pr, IDocument.DEFAULT_CONTENT_TYPE)
    
    // set the same content assistant on all partition types
    IsabellePartitions.contentTypes foreach (
        contentType => ca.setContentAssistProcessor(pr, contentType))
    
    ca.setInformationControlCreator(getInformationControlCreator(sourceViewer))
    ca
  }

  override def getPresentationReconciler(sourceViewer: ISourceViewer): IPresentationReconciler = {
    val reconciler = super.getPresentationReconciler(sourceViewer).asInstanceOf[PresentationReconciler]

    /** Sets damager/repairer for the given partition type */
    def handlePartition(partitionType: String, scanner: Option[ITokenScanner] = None) {
      
      // always initialise a scanner for the whole partition
      val partScanner = partitionScanner(partitionType)
      
      // check if another scanner was given - if so, join it on top of the partition scanner
      val fullScanner = scanner match {
        case Some(sc) => join (sc, partScanner)
        case None => partScanner
      }
      
      val dr = new DefaultDamagerRepairer(fullScanner)
      reconciler.setDamager(dr, partitionType)
      reconciler.setRepairer(dr, partitionType)
    }

    // set damager/repairer for each content type
    // add default content type
    val contentTypes = IDocument.DEFAULT_CONTENT_TYPE :: IsabellePartitions.contentTypes.toList
    
    import IsabellePartitions._
    contentTypes foreach {
      // for comments, only use the partition scanner - no need to display further scanning
      case ISABELLE_COMMENT => handlePartition(ISABELLE_COMMENT)
      // for other content types, use markup & token scanners in addition to partition scanner
      case contentType => handlePartition(contentType, Some(join(markupScanner(), tokenScanner())))
    }

    reconciler
  }
  
  private val prefs = fPreferenceStore
  
  /** Resolve the color manager and preference store values for the abstract trait */
  private trait IsabelleScanner extends AbstractIsabelleScanner {
    def colorManager = IsabelleTheoryConfiguration.this.colorManager
    
    // cannot reference parent fPreferenceStore directly here - Scala-IDE Juno crashes
    // bug reported: http://www.assembla.com/spaces/scala-ide/support/tickets/1001114-sbt-crash-for-mixed-scala-java-project
    def preferenceStore = prefs
  }

  /** Joins the scanners in a chained composite scanner */
  private def join(top: ITokenScanner, bottom: ITokenScanner): ITokenScanner =
    new ChainedTokenScanner(top, bottom)

  /** Creates a single-token partition scanner which provides tokens for different partition types */
  private def partitionScanner(partition: String): ITokenScanner =
    new SingleTokenScanner with IsabelleScanner {
      override def getToken() =
        getToken(IsabellePartitionToSyntaxClass(partition))
    }

  /** Creates a scanner for Isabelle tokens */
  private def tokenScanner(): ITokenScanner =
    new IsabelleTokenScanner(editor) with IsabelleScanner {
      override def getToken(syntax: Outer_Syntax, token: isabelle.Token) =
        getToken(IsabelleTokenToSyntaxClass(syntax, token))
    }
  
  /** Creates a scanner for Isabelle markup information */
  private def markupScanner(): ITokenScanner =
    new IsabelleMarkupScanner(editor) with IsabelleScanner {
      override def getToken(markupType: String) =
        getToken(IsabelleMarkupToSyntaxClass(markupType))
    }

  override def getHyperlinkDetectorTargets(sourceViewer: ISourceViewer): Map[String, IAdaptable] = {

    val targets = super.getHyperlinkDetectorTargets(sourceViewer).asInstanceOf[Map[String, IAdaptable]]

    // mark the editor as valid target for Isabelle Theory hyperlink detectors
    // (attaches the detector to the editor)
    targets.put(ISABELLE_THEORY_HYPERLINK_TARGET, editor);
    targets;
  }

}
