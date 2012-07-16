package isabelle.eclipse.ui.preferences

import isabelle.eclipse.ui.IsabelleUIPlugin
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.preference.PreferenceConverter
import org.eclipse.jface.resource.ResourceManager
import org.eclipse.jface.text.TextAttribute
import org.eclipse.swt.SWT

/** Definition of a syntax class that can be stored/read from a preference store.
  * <p>
  * Adapted from scala.tools.eclipse.properties.ScalaSyntaxClass
  * </p>
  * 
  * @author Andrius Velykis 
  */
case class IsabelleSyntaxClass(displayName: String, baseName: String) {

  import IsabelleSyntaxClasses._
  
  val baseKey = IsabelleUIPlugin.PLUGIN_ID + "." + baseName
  
  def colourKey = baseKey + COLOUR_SUFFIX
  def boldKey = baseKey + BOLD_SUFFIX
  def italicKey = baseKey + ITALIC_SUFFIX
  def underlineKey = baseKey + UNDERLINE_SUFFIX
  def strikethroughKey = baseKey + STRIKETHROUGH_SUFFIX

  def getTextAttribute(resourceManager: ResourceManager, preferenceStore: IPreferenceStore): TextAttribute = {
    val colour = resourceManager.createColor(PreferenceConverter.getColor(preferenceStore, colourKey))
    val style: Int = makeStyle(preferenceStore.getBoolean(boldKey), preferenceStore.getBoolean(italicKey),
      preferenceStore.getBoolean(strikethroughKey), preferenceStore.getBoolean(underlineKey))
    val backgroundColour = null
    new TextAttribute(colour, backgroundColour, style)
  }

  private def makeStyle(bold: Boolean, italic: Boolean, strikethrough: Boolean, underline: Boolean): Int = {
    var style = SWT.NORMAL
    if (bold) style |= SWT.BOLD
    if (italic) style |= SWT.ITALIC
    if (strikethrough) style |= TextAttribute.STRIKETHROUGH
    if (underline) style |= TextAttribute.UNDERLINE
    style
  }
  
}