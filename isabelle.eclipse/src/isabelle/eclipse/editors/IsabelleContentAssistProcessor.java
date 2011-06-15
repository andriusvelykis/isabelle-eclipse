package isabelle.eclipse.editors;

import java.util.ArrayList;
import java.util.List;

import isabelle.eclipse.IsabelleEclipseImages;
import isabelle.scala.SessionFacade;
import isabelle.scala.SessionFacade.CompletionInfo;
import isabelle.scala.SessionFacade.DecodedCompletion;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;

public class IsabelleContentAssistProcessor implements IContentAssistProcessor {

//	private static final Pattern OPERATOR_TAG_REGEX = Pattern.compile("\\\\<[^<]+?>");
	
	private final IContextInformation[] NO_CONTEXTS = { };
	
    private String lastError = null;
    
    private final TheoryEditor editor;
    
	public IsabelleContentAssistProcessor(TheoryEditor editor) {
		this.editor = editor;
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		
		System.out.println("Requesting completion");
		
		SessionFacade session = editor.getIsabelleSession();
		if (session == null) {
			lastError = "No Isabelle session is running";
			return null;
		}
		
		IDocument document = viewer.getDocument();

		try {
			
			int line = document.getLineOfOffset(offset);
			int start = document.getLineOffset(line);
			String text = document.get(start, offset - start);

			CompletionInfo completion = session.getCompletion(text, true);
			ICompletionProposal[] proposals = null;
			if (completion != null) {

				String word = completion.getWord();
				List<DecodedCompletion> completions = completion.getCompletions();
				
				proposals = buildProposals(word, completions, offset);
			}

			lastError = null;

			return proposals;
		} catch (BadLocationException e) {
			lastError = e.getMessage();
			return null;
		}
	}

	public static CompletionInfo getCompletion(SessionFacade session, IDocument document, int offset)
			throws BadLocationException {
		
		int line = document.getLineOfOffset(offset);
		int start = document.getLineOffset(line);
		String text = document.get(start, offset - start);
		
//		if (isTagEnd(text)) {
//			// Problem with Isabelle parser - if tag is at the end, remove last '>' symbol
//			text = text.substring(0, text.length() - 1);
//		}
		
		CompletionInfo completion = session.getCompletion(text, true);
		return completion;
	}
	
//	private static boolean isTagEnd(String text) {
//		
//		if (text.endsWith(">")) {
//			Matcher matcher = OPERATOR_TAG_REGEX.matcher(text);
//			int lastMatched = -1;
//			while (matcher.find()) {
//				lastMatched = matcher.end();
//			}
//			
//			if (lastMatched >= 0 && lastMatched == text.length()) {
//				return true;
//			}
//		}
//		
//		return false;
//	}
	
	private ICompletionProposal[] buildProposals(String word, List<DecodedCompletion> completions, int offset) {
		
		int replaceOffset = offset - word.length();
		
		List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
		
		for (DecodedCompletion completion : completions) {
			
			String replaceStr = completion.getDecoded();
			
//			if (word.equals(replaceStr)) {
//				// skip?
//				continue;
//			}
			
			String displayStr = getCompletionDisplayString(completion, replaceStr);
			Image image = IsabelleEclipseImages.getImage(IsabelleEclipseImages.IMG_CONTENT_ASSIST);
	    	proposals.add(new CompletionProposal(replaceStr, replaceOffset, word.length(), replaceStr.length(), image, displayStr, null, null));
	    }
		
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	private String getCompletionDisplayString(DecodedCompletion completion, String replaceStr) {
		
		String fullStr = completion.getCompletion();
		if (replaceStr.equals(fullStr)) {
			return replaceStr;
		}
		
		return replaceStr + " : " + fullStr; 
	}
	
	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return NO_CONTEXTS;
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[] { '\\' };
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	@Override
	public String getErrorMessage() {
		return lastError;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

}
