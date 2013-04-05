package isabelle.eclipse.ui.preferences

import org.eclipse.core.runtime.preferences.DefaultScope
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.resource.StringConverter
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.RGB
import org.eclipse.ui.preferences.ScopedPreferenceStore

import isabelle.eclipse.ui.internal.IsabelleUIPlugin
import isabelle.eclipse.ui.preferences.IsabelleSyntaxClasses._


/** Initialises preferences for syntax colours (and styles).
  * The syntax styles are defined in {@link IsabelleSyntaxClasses}.
  *
  * @author Andrius Velykis
  */
object ColorPreferenceInitializer {

  private def defaultPrefs: IPreferenceStore =
    new ScopedPreferenceStore(DefaultScope.INSTANCE, IsabelleUIPlugin.plugin.pluginId)

  def initializeDefaultPreferences() {
    putSyntaxColoringPreferences(defaultPrefs)
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
      lazy val WHITE = new RGB(255, 255, 255)
      lazy val BLACK = new RGB(0, 0, 0)
      val defaultForegroundColour = StringConverter.asString(foreground getOrElse BLACK)
      prefs.setValue(syntaxClass.foregroundColorKey, defaultForegroundColour)
      prefs.setValue(syntaxClass.foregroundColorEnabledKey, foreground.isDefined)
      val defaultBackgroundColour = StringConverter.asString(background getOrElse WHITE)
      prefs.setValue(syntaxClass.backgroundColorKey, defaultBackgroundColour)
      prefs.setValue(syntaxClass.backgroundColorEnabledKey, background.isDefined)
      prefs.setValue(syntaxClass.boldKey, bold)
      prefs.setValue(syntaxClass.italicKey, italic)
      prefs.setValue(syntaxClass.strikethroughKey, strikethrough)
      prefs.setValue(syntaxClass.underlineKey, underline)
      underlineStyle foreach (style => prefs.setValue(syntaxClass.underlineStyleKey, style))
    }

  def putSyntaxColoringPreferences(implicit prefs: IPreferenceStore) {
    
    val black = rgb(0, 0, 0)
    val red = rgb(255, 0, 0)
    val green = rgb(0, 128, 0)
    val blue = rgb(0, 0, 255)
    val chocolate = rgb(210, 105, 30)
    val darkRed = rgb(139, 0, 0)
    val keyword1 = rgb(127, 0, 85)
    val comment = rgb(63, 127, 95)
    val quoted = rgb(240, 240, 240)
    
    setDefaultsForSyntaxClass(COMMENT, comment)
    setDefaultsForSyntaxClass(VERBATIM, rgb(63, 95, 191))//, quoted)
    setDefaultsForSyntaxClass(STRING, black, quoted)
    setDefaultsForSyntaxClass(KEYWORD, keyword1, bold = true)
    setDefaultsForSyntaxClass(KEYWORD2, keyword1, bold = true)
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
    setDefaultsForSyntaxClass(ANTIQ, rgb(63, 63, 191))
    setDefaultsForSyntaxClass(ML_KEYWORD, keyword1, bold = true)
    setDefaultsForSyntaxClass(ML_NUMERAL, blue)
    setDefaultsForSyntaxClass(ML_STRING, blue)
    setDefaultsForSyntaxClass(ML_COMMENT, comment)
    
    setDefaultsForSyntaxClass(CMD, keyword1, bold = true)
    setDefaultsForSyntaxClass(CMD_SCRIPT, keyword1)
    setDefaultsForSyntaxClass(CMD_GOAL, keyword1, bold = true)

    setDefaultsForSyntaxClass(ACTIVE, underline = true, underlineStyle = Some(SWT.UNDERLINE_LINK))
    setDefaultsForSyntaxClass(DIALOG_SELECTED,
      underline = true, underlineStyle = Some(SWT.UNDERLINE_DOUBLE))
  }


  def putSyntaxColoringPreferencesJEdit(implicit prefs: IPreferenceStore) {
    
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
