package isabelle.eclipse.editors;

import isabelle.Text.Info;
import isabelle.eclipse.editors.IsabelleMarkup.TokenType;
import isabelle.scala.SnapshotFacade.NamedData;

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

public class IsabelleTheoryConfiguration extends TextSourceViewerConfiguration {

	private final TheoryEditor editor;
	private final IsabelleTokenScanner isabelleScanner;

	public IsabelleTheoryConfiguration(TheoryEditor editor, final ColorManager colorManager) {
		super(EditorsUI.getPreferenceStore());
		
		this.editor = editor;
		this.isabelleScanner = new IsabelleTokenScanner(editor) {

			@Override
			protected IToken createToken(Info<NamedData<TokenType>> tokenInfo) {
				return new Token(new TextAttribute(colorManager.getColor(
						getTokenColor(tokenInfo.info().getData()))));
			}
			
		};
		
	}
	
	@Override
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		
	    ContentAssistant ca = new ContentAssistant();
	    IContentAssistProcessor pr = new IsabelleContentAssistProcessor(editor);
	    ca.setContentAssistProcessor(pr, IDocument.DEFAULT_CONTENT_TYPE);
	    ca.setInformationControlCreator(getInformationControlCreator(sourceViewer));
	    return ca;
	}

//	@Override
//	public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
//		
//		IAutoEditStrategy[] strategies = super.getAutoEditStrategies(sourceViewer, contentType);
//		IAutoEditStrategy[] addedStrats = new IAutoEditStrategy[strategies.length + 1];
//		
//		addedStrats[0] = new IsabelleAutoEditStrategy(editor);
//		System.arraycopy(strategies, 0, addedStrats, 1, strategies.length);
//		
//		return addedStrats;
//	}

	private RGB getTokenColor(TokenType tokenType) {
		
		switch(tokenType) {
		case COMMENT1:
		case COMMENT3:
		case COMMENT4:
			return IXMLColorConstants.XML_COMMENT;
		case KEYWORD1:
		case KEYWORD2:
		case KEYWORD3:
			return IXMLColorConstants.TAG;
		case LITERAL1:
		case LITERAL3:
		case LITERAL4:
			return IXMLColorConstants.STRING;
		}
		
		return IXMLColorConstants.DEFAULT;
	}
	
//	@Override
//	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
//		return IsabellePartitioner.ISABELLE_PARTITIONING;
////		return super.getConfiguredDocumentPartitioning(sourceViewer);
//	}
//
//	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
//		String[] names = TokenType.getNames();
//		String[] types = new String[names.length + 1];
//		
//		types[0] = IDocument.DEFAULT_CONTENT_TYPE;
//		System.arraycopy(names, 0, types, 1, names.length);
//		
//		return types;
////		return new String[] {
////			IDocument.DEFAULT_CONTENT_TYPE,
////			XMLPartitionScanner.XML_COMMENT,
////			XMLPartitionScanner.XML_TAG };
//	}
//	
//	public ITextDoubleClickStrategy getDoubleClickStrategy(
//		ISourceViewer sourceViewer,
//		String contentType) {
//		if (doubleClickStrategy == null)
//			doubleClickStrategy = new XMLDoubleClickStrategy();
//		return doubleClickStrategy;
//	}
//
//	protected XMLScanner getXMLScanner() {
//		if (scanner == null) {
//			scanner = new XMLScanner(colorManager);
//			scanner.setDefaultReturnToken(
//				new Token(
//					new TextAttribute(
//						colorManager.getColor(IXMLColorConstants.DEFAULT))));
//		}
//		return scanner;
//	}
//	protected XMLTagScanner getXMLTagScanner() {
//		if (tagScanner == null) {
//			tagScanner = new XMLTagScanner(colorManager);
//			tagScanner.setDefaultReturnToken(
//				new Token(
//					new TextAttribute(
//						colorManager.getColor(IXMLColorConstants.TAG))));
//		}
//		return tagScanner;
//	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = (PresentationReconciler) super.getPresentationReconciler(sourceViewer);
//		reconciler.setDocumentPartitioning(IsabellePartitioner.ISABELLE_PARTITIONING);

		DefaultDamagerRepairer dr =
			new DefaultDamagerRepairer(isabelleScanner);
		
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
		
//		for (TokenType tokenType : TokenType.values()) {
//			
//			NonRuleBasedDamagerRepairer ndr =
//			new NonRuleBasedDamagerRepairer(
//				new TextAttribute(
//					colorManager.getColor(getTokenColor(tokenType))));
//			
//			reconciler.setDamager(ndr, tokenType.name());
//			reconciler.setRepairer(ndr, tokenType.name());	
//		}
		
//		reconciler.setDamager(dr, XMLPartitionScanner.XML_TAG);
//		reconciler.setRepairer(dr, XMLPartitionScanner.XML_TAG);
//
//		dr = new DefaultDamagerRepairer(getXMLScanner());
//		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
//		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
//
//		NonRuleBasedDamagerRepairer ndr =
//			new NonRuleBasedDamagerRepairer(
//				new TextAttribute(
//					colorManager.getColor(IXMLColorConstants.XML_COMMENT)));
//		reconciler.setDamager(ndr, XMLPartitionScanner.XML_COMMENT);
//		reconciler.setRepairer(ndr, XMLPartitionScanner.XML_COMMENT);

		return reconciler;
	}

}