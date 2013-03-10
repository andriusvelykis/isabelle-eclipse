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
class FontHeightChangeCommand(step: Int) extends AbstractHandler {

  @throws[ExecutionException]
  override def execute(event: ExecutionEvent): AnyRef = {

    val isabelleFont = IsabelleUIPreferences.ISABELLE_FONT
    // use theme font registry, otherwise preferences do not reflect change
    val fontRegistry = PlatformUI.getWorkbench.getThemeManager.getCurrentTheme.getFontRegistry

    val isaFontDescriptor = fontRegistry.getDescriptor(isabelleFont)
    
    // make sure the font does not go below 1
    val min = isaFontDescriptor.getFontData exists (_.getHeight + step < 1)
    if (!min) {
      val modifiedFont = isaFontDescriptor.increaseHeight(step).getFontData

      // TODO set descendants (fonts that use Isabelle as default substitution)
      PreferenceConverter.putValue(JFacePreferences.getPreferenceStore, isabelleFont, modifiedFont)
      fontRegistry.put(isabelleFont, modifiedFont)
    }
    
    null
  }
  
}

/**
 * Instance of height change command that increases Isabelle font by 1 point.
 * 
 * @author Andrius Velykis
 */
class FontIncreaseCommand extends FontHeightChangeCommand(1)

/**
 * Instance of height change command that decreases Isabelle font by 1 point.
 * 
 * @author Andrius Velykis
 */
class FontDecreaseCommand extends FontHeightChangeCommand(-1)
