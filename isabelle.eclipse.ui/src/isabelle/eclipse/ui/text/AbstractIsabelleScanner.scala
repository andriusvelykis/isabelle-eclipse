package isabelle.eclipse.ui.text

import org.eclipse.jface.text.IDocument
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.text.rules.ITokenScanner
import org.eclipse.jface.text.rules.IToken
import org.eclipse.jface.text.rules.Token
import org.eclipse.jface.text.source.ISharedTextColors
import org.eclipse.jface.util.PropertyChangeEvent

import isabelle.eclipse.ui.preferences.IsabelleSyntaxClass
import isabelle.eclipse.ui.preferences.IsabelleSyntaxClasses


/** Adapted from scala.tools.eclipse.lexical.AbstractScalaScanner
  * 
  * @author Andrius Velykis 
  */
trait AbstractIsabelleScanner extends ITokenScanner {

  protected def colorManager: ISharedTextColors

  protected def preferenceStore: IPreferenceStore

  private var tokens: Map[IsabelleSyntaxClass, Token] = Map()

  protected def getToken(syntaxClass: IsabelleSyntaxClass): IToken = syntaxClass match {
    case IsabelleSyntaxClasses.UNDEFINED => Token.UNDEFINED
    case _ => tokens.getOrElse(syntaxClass, createToken(syntaxClass))
  }

  private def createToken(syntaxClass: IsabelleSyntaxClass) = {
    val token = new Token(getTextAttribute(syntaxClass))
    tokens = tokens + (syntaxClass -> token)
    token
  }

  def adaptToPreferenceChange(event: PropertyChangeEvent) =
    for ((syntaxClass, token) <- tokens)
      token.setData(getTextAttribute(syntaxClass))

  private def getTextAttribute(syntaxClass: IsabelleSyntaxClass) = 
    syntaxClass.getTextAttribute(colorManager, preferenceStore)
  
}
