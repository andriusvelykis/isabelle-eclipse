package isabelle.eclipse.editors;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import isabelle.Outer_Syntax;

import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;

import scala.collection.JavaConversions;


public class IsabelleTokenScanner extends RuleBasedScanner {

	private final TheoryEditor editor;
	
	private final Queue<isabelle.Token> pendingTokens = new LinkedList<isabelle.Token>();
	
	public IsabelleTokenScanner(TheoryEditor editor) {
		this.editor = editor;
	}
	
	/**
	 * Disallow setting the rules since this scanner
	 * exclusively uses predicate rules.
	 *
	 * @param rules the sequence of rules controlling this scanner
	 */
	@Override
	public void setRules(IRule[] rules) {
		throw new UnsupportedOperationException();
	}

	/*
	 * @see ITokenScanner#nextToken()
	 */
	@Override
	public IToken nextToken() {

		fTokenOffset= fOffset;
		fColumn= UNDEFINED;
		
		/*
		 * We want to reuse Isabelle's tokenizer, so what we do is the following:
		 * 1. Read everything in the range to a String (#readContent())
		 * 2. Tokenize that in Isabelle, via #syntax.scan_context()
		 * 3. Unread everything, coming back to the original state
		 * 3. Collect all tokens as "pending", and read one by one, pushing the 
		 *    Eclipse tokenizer forward accordingly
		 */
		
		// check if there are pending tokens already, then just update the tokenizer accordingly
		IToken pendingToken = readPendingToken();
		if (pendingToken != null) {
			return pendingToken;
		}
		
		if (read() == EOF) {
			return Token.EOF;
		}
		unread();
		
		// read everything until the end of the range
		StringBuilder remainingText = readContent(fRangeEnd - fTokenOffset);
		if (remainingText.length() == 0) {
			return fDefaultReturnToken;
		}
		
		// only use Isabelle's tokenizer if session is setup
		DocumentModel isabelleModel = editor.getIsabelleModel();
		if (isabelleModel == null || !isabelleModel.getSession().is_ready()) {
			return Token.UNDEFINED;
		}
		
		// now unread all that has been read
		unread(remainingText.length());
		
		// Ask Isabelle to tokenize the text
		// TODO review (token_markup.scala)
		Outer_Syntax syntax = isabelleModel.getSession().current_syntax();
		List<isabelle.Token> tokens = JavaConversions.seqAsJavaList(
				syntax.scan_context(remainingText, isabelle.Scan$Finished$.MODULE$)._1());
		pendingTokens.addAll(tokens);
		
		// check if anything has been read
		pendingToken = readPendingToken();
		if (pendingToken != null) {
			return pendingToken;
		} else {
			// nothing added to the pending list - return default
			return fDefaultReturnToken;
		}
	}
	
	private IToken readPendingToken() {
		isabelle.Token pendingToken = pendingTokens.poll();
		if (pendingToken != null) {
			// read the token
			String sourceStr = pendingToken.source();
			read(sourceStr.length());
			return createToken(pendingToken);
		}
		
		return null;
	}
	
	private StringBuilder readContent(int length) {
		StringBuilder out = new StringBuilder();

		for (int i = 0; i < length; i++) {
			int c = read();
			if (c == EOF) {
				unread();
				break;
			} else {
				out.append((char) c);
			}
		}
		
		return out;
	}
	
	protected IToken createToken(isabelle.Token tokenInfo) {
		return new Token(tokenInfo);
	}
	
	private void read(int length) {
		for (int i = 0; i < length; i++) {
			read();
		}
	}
	
	private void unread(int length) {
		for (int i = 0; i < length; i++) {
			unread();
		}
	}

}
