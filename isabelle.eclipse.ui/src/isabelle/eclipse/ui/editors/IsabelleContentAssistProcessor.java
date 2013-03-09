package isabelle.eclipse.ui.editors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import isabelle.Completion;
import isabelle.Symbol;
import isabelle.eclipse.core.text.DocumentModel;
import isabelle.eclipse.ui.internal.IsabelleImages;

import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;

import scala.Option;
import scala.Tuple2;
import scala.collection.JavaConversions;

public class IsabelleContentAssistProcessor implements IContentAssistProcessor {

//	private static final Pattern OPERATOR_TAG_REGEX = Pattern.compile("\\\\<[^<]+?>");
	
	private final IContextInformation[] NO_CONTEXTS = { };
	
    private String lastError = null;
    
    private final TheoryEditor editor;
    private final ResourceManager resourceManager;
    
	public IsabelleContentAssistProcessor(TheoryEditor editor, ResourceManager resourceMgr) {
		this.editor = editor;
		this.resourceManager = resourceMgr;
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		
		System.out.println("Requesting completion");
		
		Option<DocumentModel> isabelleModelOpt = editor.isabelleModel();
		if (isabelleModelOpt.isEmpty()) {
			lastError = "No Isabelle session is running";
			return null;
		}
		DocumentModel isabelleModel = isabelleModelOpt.get();
		IDocument document = viewer.getDocument();

		try {
			
			int line = document.getLineOfOffset(offset);
			int start = document.getLineOffset(line);
			String text = document.get(start, offset - start);
			
			List<CompletionProposalInfo> proposalInfos = getCompletions(isabelleModel, text);
			if (proposalInfos.isEmpty()) {
				return null;
			}
			
			Image image = resourceManager.createImageWithDefault(IsabelleImages.CONTENT_ASSIST());
			
			List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();

			for (CompletionProposalInfo info : proposalInfos) {
				
				String word = info.getWord();
				String replaceStr = info.getReplaceText();
				String fullStr = info.getFullText();
				
				int replaceOffset = offset - word.length();
				
				String displayStr = getCompletionDisplayString(fullStr, replaceStr);
				proposals.add(new CompletionProposal(replaceStr, replaceOffset, word.length(), replaceStr.length(), 
						image, displayStr, null, null));
			}
			
			lastError = null;
			
			// TODO sort?
			return proposals.toArray(new ICompletionProposal[proposals.size()]);
			
		} catch (BadLocationException e) {
			lastError = e.getMessage();
			return null;
		}
	}
	
	public static List<CompletionProposalInfo> getCompletions(DocumentModel isabelleModel, String text){

		Completion completion = isabelleModel.session().recent_syntax().completion();
		Option<Tuple2<String, scala.collection.immutable.List<String>>> rawProposalsOpt = 
				completion.complete(text);

		if (rawProposalsOpt.isEmpty()) {
			return Collections.emptyList();
		}

		Tuple2<String, scala.collection.immutable.List<String>> rawProposals = rawProposalsOpt.get();

		String foundWord = rawProposals._1();

		List<CompletionProposalInfo> proposals = new ArrayList<CompletionProposalInfo>();

		for (String fullStr : JavaConversions.asJavaIterable(rawProposals._2())) {
			// decode, e.g. and replace --> with x-symbol
			String replaceStr = Symbol.decode(fullStr);

			proposals.add(new CompletionProposalInfo(foundWord, replaceStr, fullStr));
		}

		// TODO sort?
		return proposals;
	}

	private static String getCompletionDisplayString(String fullStr, String replaceStr) {
		
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
	
	public static class CompletionProposalInfo {
		private final String word;
		private final String replaceText;
		private final String fullText;
		
		public CompletionProposalInfo(String word, String replaceText, String fullText) {
			super();
			this.word = word;
			this.replaceText = replaceText;
			this.fullText = fullText;
		}

		public String getWord() {
			return word;
		}

		public String getReplaceText() {
			return replaceText;
		}

		public String getFullText() {
			return fullText;
		}
	}

}
