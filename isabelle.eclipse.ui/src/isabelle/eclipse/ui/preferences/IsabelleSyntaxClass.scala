package isabelle.eclipse.ui.preferences

import org.eclipse.jface.preference.{IPreferenceStore, PreferenceConverter}
import org.eclipse.jface.resource.ResourceManager
import org.eclipse.jface.text.TextAttribute
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Color

import isabelle.eclipse.ui.internal.IsabelleUIPlugin
import isabelle.eclipse.ui.editors.ExtendedTextAttribute


/** Definition of a syntax class that can be stored/read from a preference store.
  * <p>
  * Adapted from scala.tools.eclipse.properties.ScalaSyntaxClass
  * </p>
  * 
  * @author Andrius Velykis 
  */
case class IsabelleSyntaxClass(displayName: String, baseName: String) {

  import IsabelleSyntaxClasses._
  
  val baseKey = IsabelleUIPlugin.plugin.pluginId + "." + baseName
  
  def foregroundColorKey = baseKey + COLOR_SUFFIX
  def foregroundColorEnabledKey = baseKey + COLOR_ENABLED_SUFFIX
  def backgroundColorKey = baseKey + BACKGROUND_COLOR_SUFFIX
  def backgroundColorEnabledKey = baseKey + BACKGROUND_COLOR_ENABLED_SUFFIX
  def boldKey = baseKey + BOLD_SUFFIX
  def italicKey = baseKey + ITALIC_SUFFIX
  def underlineKey = baseKey + UNDERLINE_SUFFIX
  def underlineStyleKey = baseKey + UNDERLINE_STYLE_SUFFIX
  def strikethroughKey = baseKey + STRIKETHROUGH_SUFFIX

  def getTextAttribute(resourceManager: ResourceManager,
                       preferenceStore: IPreferenceStore): TextAttribute = {

    def prefColor(colorKey: String): Option[Color] =
      if (preferenceStore.contains(colorKey))
        Some(resourceManager.createColor(PreferenceConverter.getColor(preferenceStore, colorKey)))
      else None

    val foregroundEnabled = preferenceStore.getBoolean(foregroundColorEnabledKey)
    val foreground = if (foregroundEnabled) prefColor(foregroundColorKey) else None

    val backgroundEnabled = preferenceStore.getBoolean(backgroundColorEnabledKey)
    val background = if (backgroundEnabled) prefColor(backgroundColorKey) else None

    val style: Int = makeStyle(
      preferenceStore.getBoolean(boldKey),
      preferenceStore.getBoolean(italicKey),
      preferenceStore.getBoolean(strikethroughKey),
      preferenceStore.getBoolean(underlineKey))
    
    val underlineStyle = preferenceStore.getInt(underlineStyleKey)

    new ExtendedTextAttribute(foreground.orNull, background.orNull, style, null, underlineStyle)
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