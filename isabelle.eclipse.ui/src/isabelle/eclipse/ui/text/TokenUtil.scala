package isabelle.eclipse.ui.text

import org.eclipse.jface.text.TextAttribute
import org.eclipse.jface.text.rules.{IToken, Token}


/**
 * Utilities related to text tokens.
 * 
 * @author Andrius Velykis
 */
object TokenUtil {

  object Merge {
    
    def takeTopToken(top: IToken, bottom: IToken): IToken = top

    def mergeTextTokens(top: IToken, bottom: IToken): IToken =
      // special cases for EOF - use them as merged
      if (top.isEOF) top
      else if (bottom.isEOF) bottom
      else (textAttr(top), textAttr(bottom)) match {
        
        case (Some(topAttr), Some(bottomAttr)) =>
          new Token(mergeTextAttributes(topAttr, bottomAttr))
          
        case (Some(topAttr), _) => top
        
        case (_, Some(bottomAttr)) => bottom
        
        // cachall - just return the top
        case _ => top
      }
    
    private def textAttr(token: IToken): Option[TextAttribute] = token.getData match {
      case attr: TextAttribute => Some(attr)
      case _ => None
    }
    
    private def mergeTextAttributes(top: TextAttribute, bottom: TextAttribute): TextAttribute = {
      
      val foreground = Option(top.getForeground) orElse Option(bottom.getForeground)
      val background = Option(top.getBackground) orElse Option(bottom.getBackground)
      val style = bottom.getStyle | top.getStyle
      val font = Option(top.getFont) orElse Option(bottom.getFont)
      
      new TextAttribute(foreground.orNull, background.orNull, style, font.orNull)
    }
  }
  
}
