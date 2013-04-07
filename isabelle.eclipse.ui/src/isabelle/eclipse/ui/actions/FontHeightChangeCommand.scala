package isabelle.eclipse.ui.actions

import org.eclipse.core.commands.{AbstractHandler, ExecutionEvent, ExecutionException}
import org.eclipse.jface.preference.{JFacePreferences, PreferenceConverter}
import org.eclipse.ui.PlatformUI

import isabelle.eclipse.ui.preferences.IsabelleUIPreferences


/**
 * A command that changes the Isabelle font size.
 *
 * @author Andrius Velykis
 */
class FontHeightChangeCommand(change: Int => Int) extends AbstractHandler {

  /** Minimum allowed font size */
  private def MIN = 5

  /** Maximum allowed font size */
  private def MAX = 250

  @throws[ExecutionException]
  override def execute(event: ExecutionEvent): AnyRef = {

    val isabelleFont = IsabelleUIPreferences.ISABELLE_FONT
    // use theme font registry, otherwise preferences do not reflect change
    val fontRegistry = PlatformUI.getWorkbench.getThemeManager.getCurrentTheme.getFontRegistry

    val isaFontData = fontRegistry.getDescriptor(isabelleFont).getFontData
    
    // make sure the font does not go beyond minimum and maximum
    val canChange = isaFontData forall (d => validChange(d.getHeight))
    if (canChange) {
      
      // modify the font (working on a copy here)
      isaFontData foreach (d => d.setHeight(change(d.getHeight)))

      // TODO set descendants (fonts that use Isabelle as default substitution)
      PreferenceConverter.putValue(JFacePreferences.getPreferenceStore, isabelleFont, isaFontData)
      fontRegistry.put(isabelleFont, isaFontData)
    }
    
    null
  }

  private def validChange(height: Int): Boolean = {
    val nextHeight = change(height)
    nextHeight >= MIN && nextHeight <= MAX
  }
  
}

/**
 * Instance of height change command that increases Isabelle font by 1/10 of the size
 * 
 * @author Andrius Velykis
 */
class FontIncreaseCommand extends FontHeightChangeCommand( h => h + (h / 10 max 1) )

/**
 * Instance of height change command that decreases Isabelle font by 1/10 of the size
 * 
 * @author Andrius Velykis
 */
class FontDecreaseCommand extends FontHeightChangeCommand( h => h - (h / 10 max 1) )
