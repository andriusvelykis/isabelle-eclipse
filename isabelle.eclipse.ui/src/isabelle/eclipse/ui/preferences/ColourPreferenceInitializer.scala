package isabelle.eclipse.ui.preferences

import isabelle.eclipse.ui.preferences.IsabelleSyntaxClasses._
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer
import org.eclipse.swt.graphics.RGB
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.resource.StringConverter
import org.eclipse.ui.editors.text.EditorsUI
import isabelle.eclipse.ui.IsabelleUIPlugin
import org.eclipse.swt.graphics.Color

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
    colour: RGB,
    bold: Boolean = false,
    italic: Boolean = false,
    strikethrough: Boolean = false,
    underline: Boolean = false)(implicit prefs: IPreferenceStore) =
    {
      prefs.setDefault(syntaxClass.colourKey, StringConverter.asString(colour))
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
    
    setDefaultsForSyntaxClass(COMMENT, new RGB(204, 0, 0))
    setDefaultsForSyntaxClass(VERBATIM, black)
    setDefaultsForSyntaxClass(STRING, black)
    setDefaultsForSyntaxClass(KEYWORD, keyword2, bold = true)
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
    setDefaultsForSyntaxClass(INNER_COMMENT, darkRed)
    setDefaultsForSyntaxClass(DYN_FACT, new RGB(123, 164, 40))
    setDefaultsForSyntaxClass(ANTIQ, blue)
    setDefaultsForSyntaxClass(ML_KEYWORD, keyword1, bold = true)
    setDefaultsForSyntaxClass(ML_NUMERAL, red)
    setDefaultsForSyntaxClass(ML_STRING, chocolate)
    setDefaultsForSyntaxClass(ML_COMMENT, darkRed)
    setDefaultsForSyntaxClass(ML_BAD, new RGB(255, 106, 106))
    
    setDefaultsForSyntaxClass(CMD, keyword1, bold = true)
    setDefaultsForSyntaxClass(CMD_SCRIPT, new RGB(2, 185, 2))
    setDefaultsForSyntaxClass(CMD_GOAL, keyword2, bold = true)
  }

}