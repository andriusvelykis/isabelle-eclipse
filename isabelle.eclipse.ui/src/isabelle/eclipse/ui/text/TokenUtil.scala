package isabelle.eclipse.ui.text

import org.eclipse.jface.text.rules.IToken

/**
 * Utilities related to text tokens.
 * 
 * @author Andrius Velykis
 */
object TokenUtil {

  object Merge {
    
    def takeTopToken(top: IToken, bottom: IToken): IToken = top
  
    def mergeTextTokens(top: IToken, bottom: IToken): IToken = {
      
      top
      
    }
    
  }
  
}
