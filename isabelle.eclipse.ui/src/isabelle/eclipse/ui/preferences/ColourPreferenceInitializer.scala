package isabelle.eclipse.ui.preferences

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.resource.StringConverter
import org.eclipse.swt.graphics.RGB

import isabelle.eclipse.ui.IsabelleUIPlugin
import isabelle.eclipse.ui.preferences.IsabelleSyntaxClasses._


/** Initialises preferences for syntax colours (and styles).
  * The syntax styles are defined in {@link IsabelleSyntaxClasses}.
  *
  * @author Andrius Velykis
  */
class ColourPreferenceInitializer extends AbstractPreferenceInitializer {

  def initializeDefaultPreferences() {
    setDefaultsForSyntaxClasses(IsabelleUIPlugin.getPreferences())
  }
  
  private def setDefaultsForSyntaxClass(
    syntaxClass: IsabelleSyntaxClass,
    foreground: RGB,
    background: Option[RGB] = None,
    bold: Boolean = false,
    italic: Boolean = false,
    strikethrough: Boolean = false,
    underline: Boolean = false)(implicit prefs: IPreferenceStore) =
    {
      prefs.setDefault(syntaxClass.foregroundKey, StringConverter.asString(foreground))
      background foreach (bg =>
        prefs.setDefault(syntaxClass.backgroundKey, StringConverter.asString(bg)))
      prefs.setDefault(syntaxClass.boldKey, bold)
      prefs.setDefault(syntaxClass.italicKey, italic)
      prefs.setDefault(syntaxClass.strikethroughKey, strikethrough)
      prefs.setDefault(syntaxClass.underlineKey, underline)
    }

  private def setDefaultsForSyntaxClasses(implicit prefs: IPreferenceStore) {
    
    val black = new RGB(0, 0, 0)
    val red = new RGB(255, 0, 0)
    val green = new RGB(0, 128, 0)
    val blue = new RGB(0, 0, 255)
    val chocolate = new RGB(210, 105, 30)
    val darkRed = new RGB(139, 0, 0)
    val keyword1 = new RGB(0, 102, 153)
    val keyword2 = new RGB(0, 153, 102)
    val keyword3 = new RGB(43, 157, 255)
    val quoted = new RGB(240, 240, 240)
    
    setDefaultsForSyntaxClass(COMMENT, new RGB(204, 0, 0))
    setDefaultsForSyntaxClass(VERBATIM, black, Some(quoted))
    setDefaultsForSyntaxClass(STRING, black, Some(quoted))
    setDefaultsForSyntaxClass(KEYWORD, keyword1, bold = true)
    setDefaultsForSyntaxClass(KEYWORD2, keyword2, bold = true)
    setDefaultsForSyntaxClass(OPERATOR, black, bold = true)
    setDefaultsForSyntaxClass(LITERAL, keyword1)
    setDefaultsForSyntaxClass(DELIMITER, black)
    setDefaultsForSyntaxClass(TYPE, new RGB(160, 32, 240))
    setDefaultsForSyntaxClass(FREE, blue)
    setDefaultsForSyntaxClass(SKOLEM, chocolate)
    setDefaultsForSyntaxClass(BOUND, green)
    setDefaultsForSyntaxClass(VAR, new RGB(0, 0, 155))
    setDefaultsForSyntaxClass(INNER_STRING, chocolate)
    setDefaultsForSyntaxClass(INNER_COMMENT, darkRed)
    setDefaultsForSyntaxClass(DYN_FACT, new RGB(123, 164, 40))
    setDefaultsForSyntaxClass(ANTIQ, blue)
    setDefaultsForSyntaxClass(ML_KEYWORD, keyword1, bold = true)
    setDefaultsForSyntaxClass(ML_NUMERAL, red)
    setDefaultsForSyntaxClass(ML_STRING, chocolate)
    setDefaultsForSyntaxClass(ML_COMMENT, darkRed)
    
    setDefaultsForSyntaxClass(CMD, keyword1, bold = true)
    setDefaultsForSyntaxClass(CMD_SCRIPT, new RGB(246, 52, 36))
    setDefaultsForSyntaxClass(CMD_GOAL, keyword3, bold = true)
  }

}