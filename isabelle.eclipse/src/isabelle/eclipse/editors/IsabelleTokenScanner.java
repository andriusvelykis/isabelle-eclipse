package isabelle.eclipse.editors;

import java.util.Iterator;

import isabelle.Text.Info;
import isabelle.Text.Range;
import isabelle.eclipse.editors.IsabelleMarkup.TokenType;
import isabelle.scala.SessionFacade;
import isabelle.scala.SnapshotFacade;
import isabelle.scala.SnapshotFacade.NamedData;

import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;


public class IsabelleTokenScanner extends RuleBasedScanner {

	private final TheoryEditor editor;
	
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

		if (read() == EOF) {
			return Token.EOF;
		}
		
		unread();
		
		if (editor != null) {
			
			SnapshotFacade snapshot = editor.getSnapshot();
			SessionFacade session = editor.getIsabelleSession();
			
			// TODO check the markup
			if (session != null && snapshot != null) {
				
//				System.out.println("Isabelle tokens");
				Iterator<Info<NamedData<TokenType>>> tokenTypes = snapshot.selectTokens(
						session.getSession().current_syntax(), 
						IsabelleMarkup.COMMAND_STYLES, 
						IsabelleMarkup.TOKEN_STYLES,
						new Range(fTokenOffset, fRangeEnd));
				
				Info<NamedData<TokenType>> nextToken = getNextToken(tokenTypes);
				
				if (nextToken != null) {
//					System.out.println("Next token found at range " + nextToken.range() + ": " + nextToken.info().getName() + ": " + nextToken.info().getData());
					read(fTokenOffset, nextToken.range().stop());
					return createToken(nextToken);
				} else {
					// read to the end
					read(fTokenOffset, fRangeEnd);
//					System.out.println("No tokens found starting from " + fTokenOffset + " to " + fRangeEnd);
					return Token.UNDEFINED;
				}
			}
		}
		
		if (read() == EOF) {
			return Token.EOF;
		}		
		
		return fDefaultReturnToken;
	}
	
	protected IToken createToken(Info<NamedData<TokenType>> tokenInfo) {
		return new Token(tokenInfo.info().getData().name());
	}
	
	private <A> Info<A> getNextToken(Iterator<Info<A>> tokenTypes) {
		
		if (!tokenTypes.hasNext()) {
			return null;
		}
		
		Info<A> tokenStartInfo = tokenTypes.next();
		A tokenId = tokenStartInfo.info();
		
		Info<A> tokenEndInfo = tokenStartInfo;
		
		while (tokenTypes.hasNext()) {
			Info<A> nextTokenInfo = tokenTypes.next();
			if (!nextTokenInfo.info().equals(tokenId)) {
				// different token ID - stop
				break;
			}
			
			// still the same token ID - continue
			tokenEndInfo = nextTokenInfo;
		}
		
		return new Info<A>(new Range(tokenStartInfo.range().start(), tokenEndInfo.range().stop()), tokenId);
	}
	
	private void read(int start, int end) {
		for (int i = start; i < end; i++) {
			read();
		}
	}
	
	
//    val start = buffer.getLineStartOffset(line)
//    val stop = start + line_segment.count
//
//    /* FIXME
//    for (text_area <- Isabelle.jedit_text_areas(buffer)
//          if Document_View(text_area).isDefined)
//      Document_View(text_area).get.set_styles()
//    */
//
//    def handle_token(style: Byte, offset: Text.Offset, length: Int) =
//      handler.handleToken(line_segment, style, offset, length, context)
//
//    val syntax = session.current_syntax()
//    val tokens = snapshot.select_markup(Text.Range(start, stop))(Isabelle_Markup.tokens(syntax))
//
//    var last = start
//    for (token <- tokens.iterator) {
//      val Text.Range(token_start, token_stop) = token.range
//      if (last < token_start)
//        handle_token(Token.COMMENT1, last - start, token_start - last)
//      handle_token(token.info getOrElse Token.NULL,
//        token_start - start, token_stop - token_start)
//      last = token_stop
//    }
//    if (last < stop) handle_token(Token.COMMENT1, last - start, stop - last)
//
//    handle_token(Token.END, line_segment.count, 0)
//    handler.setLineContext(context)

}
