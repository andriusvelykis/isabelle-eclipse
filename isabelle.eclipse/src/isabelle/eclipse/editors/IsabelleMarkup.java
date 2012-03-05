package isabelle.eclipse.editors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import isabelle.Keyword;
import isabelle.Markup;

import static isabelle.eclipse.editors.IsabelleMarkup.TokenType.*;

/**
 * @author Andrius Velykis
 * @deprecated not used at the moment? Using a new isabelle token scanner?
 */
@Deprecated
public class IsabelleMarkup {

	public enum TokenType {
		COMMENT1,
		COMMENT3,
		COMMENT4,
		DIGIT,
		FUNCTION,
		INVALID,
		KEYWORD1,
		KEYWORD2,
		KEYWORD3,
		LABEL,
		LITERAL1,
		LITERAL3,
		LITERAL4,
		OPERATOR;
		
		public static String[] getNames() {
			TokenType[] types = TokenType.values();
			String[] names = new String[types.length];
			
			for (int index = 0; index < types.length; index++) {
				names[index] = types[index].name();
			}
			
			return names;
		}
	}
	
	  /* token markup -- text styles */
	public static final Map<String, TokenType> COMMAND_STYLES = createCommandStyles();
	
	public static final Map<String, TokenType> TOKEN_STYLES = createTokenStyles();
	
	private static Map<String, TokenType> createCommandStyles() {
		HashMapWithDefault<String, TokenType> commandStyles = new HashMapWithDefault<String, TokenType>();
		commandStyles.put(Keyword.THY_END(), KEYWORD2);
		commandStyles.put(Keyword.THY_SCRIPT(), LABEL);
		commandStyles.put(Keyword.PRF_SCRIPT(), LABEL);
		commandStyles.put(Keyword.PRF_ASM(), KEYWORD3);
		commandStyles.put(Keyword.PRF_ASM_GOAL(), KEYWORD3);
		commandStyles.setDefaultValue(KEYWORD1);

		return Collections.unmodifiableMap(commandStyles);
	}
	
	private static Map<String, TokenType> createTokenStyles() {
		Map<String, TokenType> tokenStyles = new HashMap<String, TokenType>();
		// FIXME restore
//		tokenStyles.put(Markup.TCLASS(), null);
//		tokenStyles.put(Markup.TYCON(), null);
//		tokenStyles.put(Markup.FIXED_DECL(), FUNCTION);
//		tokenStyles.put(Markup.FIXED(), null);
//		tokenStyles.put(Markup.CONST_DECL(), FUNCTION);
//		tokenStyles.put(Markup.CONST(), null);
//		tokenStyles.put(Markup.FACT_DECL(), FUNCTION);
//		tokenStyles.put(Markup.FACT(), null);
//		tokenStyles.put(Markup.DYNAMIC_FACT(), LABEL);
//		tokenStyles.put(Markup.LOCAL_FACT_DECL(), FUNCTION);
//		tokenStyles.put(Markup.LOCAL_FACT(), null);
	      // inner syntax
		tokenStyles.put(Markup.TFREE(), null);
		tokenStyles.put(Markup.FREE(), null);
		tokenStyles.put(Markup.TVAR(), null);
		tokenStyles.put(Markup.SKOLEM(), null);
		tokenStyles.put(Markup.BOUND(), null);
		tokenStyles.put(Markup.VAR(), null);
		tokenStyles.put(Markup.NUM(), DIGIT);
		tokenStyles.put(Markup.FLOAT(), DIGIT);
		tokenStyles.put(Markup.XNUM(), DIGIT);
		tokenStyles.put(Markup.XSTR(), LITERAL4);
		tokenStyles.put(Markup.LITERAL(), OPERATOR);
		tokenStyles.put(Markup.INNER_COMMENT(), COMMENT1);
		tokenStyles.put(Markup.SORT(), null);
		tokenStyles.put(Markup.TYP(), null);
		tokenStyles.put(Markup.TERM(), null);
		tokenStyles.put(Markup.PROP(), null);
		tokenStyles.put(Markup.ATTRIBUTE(), null);
		tokenStyles.put(Markup.METHOD(), null);
	      // ML syntax
		tokenStyles.put(Markup.ML_KEYWORD(), KEYWORD1);
		tokenStyles.put(Markup.ML_DELIMITER(), OPERATOR);
//		tokenStyles.put(Markup.ML_IDENT(), null);
		tokenStyles.put(Markup.ML_TVAR(), null);
		tokenStyles.put(Markup.ML_NUMERAL(), DIGIT);
		tokenStyles.put(Markup.ML_CHAR(), LITERAL1);
		tokenStyles.put(Markup.ML_STRING(), LITERAL1);
		tokenStyles.put(Markup.ML_COMMENT(), COMMENT1);
		tokenStyles.put(Markup.ML_MALFORMED(), INVALID);
	      // embedded source text
		tokenStyles.put(Markup.ML_SOURCE(), COMMENT3);
		tokenStyles.put(Markup.DOC_SOURCE(), COMMENT3);
		tokenStyles.put(Markup.ANTIQ(), COMMENT4);
//		tokenStyles.put(Markup.ML_ANTIQ(), COMMENT4);
//		tokenStyles.put(Markup.DOC_ANTIQ(), COMMENT4);
	      // outer syntax
		tokenStyles.put(Markup.KEYWORD(), KEYWORD2);
		tokenStyles.put(Markup.OPERATOR(), OPERATOR);
		tokenStyles.put(Markup.COMMAND(), KEYWORD1);
//		tokenStyles.put(Markup.IDENT(), null);
		tokenStyles.put(Markup.VERBATIM(), COMMENT3);
		tokenStyles.put(Markup.COMMENT(), COMMENT1);
		tokenStyles.put(Markup.CONTROL(), COMMENT3);
		tokenStyles.put(Markup.MALFORMED(), INVALID);
		tokenStyles.put(Markup.STRING(), LITERAL3);
		tokenStyles.put(Markup.ALTSTRING(), LITERAL1);
		
		return Collections.unmodifiableMap(tokenStyles);
	}
	
}
