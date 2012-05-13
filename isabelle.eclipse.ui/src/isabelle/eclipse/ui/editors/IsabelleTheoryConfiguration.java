package isabelle.eclipse.ui.editors;

import java.util.Arrays;
import java.util.Map;

import isabelle.Token$Kind$;

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

import scala.Enumeration.Value;

public class IsabelleTheoryConfiguration extends TextSourceViewerConfiguration {

	/**
	 * The hyperlink detector target ID, as defined in plugin.xml
	 */
	private static final String ISABELLE_THEORY_HYPERLINK_TARGET = "isabelle.eclipse.ui.theoryEditor";

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

	private RGB getTokenColor(Value tokenType) {
		
		System.out.println("Token: " + tokenType);
		
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

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = (PresentationReconciler) super.getPresentationReconciler(sourceViewer);

		DefaultDamagerRepairer dr =
			new DefaultDamagerRepairer(isabelleScanner);
		
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		return reconciler;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.editors.text.TextSourceViewerConfiguration#getHyperlinkDetectorTargets(
	 * 			org.eclipse.jface.text.source.ISourceViewer)
	 */
	@Override
	protected Map<String, IAdaptable> getHyperlinkDetectorTargets(ISourceViewer sourceViewer) {
		
		@SuppressWarnings("unchecked")
		Map<String, IAdaptable> targets = super.getHyperlinkDetectorTargets(sourceViewer);
		
		// mark the editor as valid target for Isabelle Theory hyperlink detectors
		// (attaches the detector to the editor)
		targets.put(ISABELLE_THEORY_HYPERLINK_TARGET, editor);
		
		return targets;
	}

}