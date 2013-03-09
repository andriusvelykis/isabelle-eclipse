package isabelle.eclipse.ui.preferences

import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.resource.StringConverter
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.RGB

import isabelle.eclipse.ui.internal.IsabelleUIPlugin
import isabelle.eclipse.ui.preferences.IsabelleSyntaxClasses._


/** Initialises preferences for syntax colours (and styles).
  * The syntax styles are defined in {@link IsabelleSyntaxClasses}.
  *
  * @author Andrius Velykis
  */
object ColourPreferenceInitializer {

  def initializeDefaultPreferences() {
    setDefaultsForSyntaxClasses(IsabelleUIPlugin.plugin.getPreferenceStore)
  }
  
  private def setDefaultsForSyntaxClass(
    syntaxClass: IsabelleSyntaxClass,
    foreground: Option[RGB] = None,
    background: Option[RGB] = None,
    bold: Boolean = false,
    italic: Boolean = false,
    strikethrough: Boolean = false,
    underline: Boolean = false,
    underlineStyle: Option[Int] = None)(implicit prefs: IPreferenceStore) =
    {
      foreground foreach (fg =>
        prefs.setDefault(syntaxClass.foregroundKey, StringConverter.asString(fg)))
      background foreach (bg =>
        prefs.setDefault(syntaxClass.backgroundKey, StringConverter.asString(bg)))
      prefs.setDefault(syntaxClass.boldKey, bold)
      prefs.setDefault(syntaxClass.italicKey, italic)
      prefs.setDefault(syntaxClass.strikethroughKey, strikethrough)
      prefs.setDefault(syntaxClass.underlineKey, underline)
      underlineStyle foreach (style => prefs.setDefault(syntaxClass.underlineStyleKey, style))
    }

  private def setDefaultsForSyntaxClasses(implicit prefs: IPreferenceStore) {
    
    val black = rgb(0, 0, 0)
    val red = rgb(255, 0, 0)
    val green = rgb(0, 128, 0)
    val blue = rgb(0, 0, 255)
    val chocolate = rgb(210, 105, 30)
    val darkRed = rgb(139, 0, 0)
    val keyword1 = rgb(0, 102, 153)
    val keyword2 = rgb(0, 153, 102)
    val keyword3 = rgb(43, 157, 255)
    val quoted = rgb(240, 240, 240)
    
    setDefaultsForSyntaxClass(COMMENT, rgb(204, 0, 0))
    setDefaultsForSyntaxClass(VERBATIM, black, quoted)
    setDefaultsForSyntaxClass(STRING, black, quoted)
    setDefaultsForSyntaxClass(KEYWORD, keyword1, bold = true)
    setDefaultsForSyntaxClass(KEYWORD2, keyword2, bold = true)
    setDefaultsForSyntaxClass(OPERATOR, black, bold = true)
    setDefaultsForSyntaxClass(LITERAL, keyword1)
    setDefaultsForSyntaxClass(DELIMITER, black)
    setDefaultsForSyntaxClass(TYPE, rgb(160, 32, 240))
    setDefaultsForSyntaxClass(FREE, blue)
    setDefaultsForSyntaxClass(SKOLEM, chocolate)
    setDefaultsForSyntaxClass(BOUND, green)
    setDefaultsForSyntaxClass(VAR, rgb(0, 0, 155))
    setDefaultsForSyntaxClass(INNER_STRING, chocolate)
    setDefaultsForSyntaxClass(INNER_COMMENT, darkRed)
    setDefaultsForSyntaxClass(DYN_FACT, rgb(123, 164, 40))
    setDefaultsForSyntaxClass(ANTIQ, blue)
    setDefaultsForSyntaxClass(ML_KEYWORD, keyword1, bold = true)
    setDefaultsForSyntaxClass(ML_NUMERAL, red)
    setDefaultsForSyntaxClass(ML_STRING, chocolate)
    setDefaultsForSyntaxClass(ML_COMMENT, darkRed)
    
    setDefaultsForSyntaxClass(CMD, keyword1, bold = true)
    setDefaultsForSyntaxClass(CMD_SCRIPT, rgb(246, 52, 36))
    setDefaultsForSyntaxClass(CMD_GOAL, keyword3, bold = true)

    setDefaultsForSyntaxClass(ACTIVE, underline = true, underlineStyle = Some(SWT.UNDERLINE_LINK))
    setDefaultsForSyntaxClass(DIALOG_SELECTED,
      underline = true, underlineStyle = Some(SWT.UNDERLINE_DOUBLE))
  }
  
  private def rgb(r: Int, g: Int, b: Int): Option[RGB] = Some(new RGB(r, g, b))

}
