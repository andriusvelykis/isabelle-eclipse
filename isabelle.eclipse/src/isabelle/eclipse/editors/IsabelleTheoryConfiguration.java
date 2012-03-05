package isabelle.eclipse.editors;

import java.util.Arrays;

import isabelle.Token$Kind$;

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

import scala.Enumeration.Value;

public class IsabelleTheoryConfiguration extends TextSourceViewerConfiguration {

	private static final Token$Kind$ TOKEN_KIND = isabelle.Token$Kind$.MODULE$;
	
	private final TheoryEditor editor;
	private final IsabelleTokenScanner isabelleScanner;

	public IsabelleTheoryConfiguration(TheoryEditor editor, final ColorManager colorManager) {
		super(EditorsUI.getPreferenceStore());
		
		this.editor = editor;
		this.isabelleScanner = new IsabelleTokenScanner(editor) {

			@Override
			protected IToken createToken(isabelle.Token tokenInfo) {
				return new Token(new TextAttribute(colorManager.getColor(
						getTokenColor(tokenInfo.kind()))));
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

	private RGB getTokenColor(Value tokenType) {
		
		if (tokenType == TOKEN_KIND.COMMENT()) {
			return IXMLColorConstants.XML_COMMENT; 
		}
		
		if (Arrays.asList(TOKEN_KIND.COMMAND(), TOKEN_KIND.KEYWORD()).contains(tokenType)) {
			return IXMLColorConstants.TAG;
		}
		
		if (Arrays.asList(TOKEN_KIND.ALT_STRING(), TOKEN_KIND.FLOAT(),
				TOKEN_KIND.NAT(), TOKEN_KIND.STRING()).contains(tokenType)) {
			return IXMLColorConstants.STRING;
		}
		
		// TODO add all token types and proper colour options
		
		return IXMLColorConstants.DEFAULT;
	}
	
//    val COMMAND = Value("command")
//    val KEYWORD = Value("keyword")
//    val IDENT = Value("identifier")
//    val LONG_IDENT = Value("long identifier")
//    val SYM_IDENT = Value("symbolic identifier")
//    val VAR = Value("schematic variable")
//    val TYPE_IDENT = Value("type variable")
//    val TYPE_VAR = Value("schematic type variable")
//    val NAT = Value("natural number")
//    val FLOAT = Value("floating-point number")
//    val STRING = Value("string")
//    val ALT_STRING = Value("back-quoted string")
//    val VERBATIM = Value("verbatim text")
//    val SPACE = Value("white space")
//    val COMMENT = Value("comment text")
//    val UNPARSED = Value("unparsed input")
	
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