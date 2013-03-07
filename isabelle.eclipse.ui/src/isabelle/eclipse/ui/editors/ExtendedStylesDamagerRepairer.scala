package isabelle.eclipse.ui.editors

import org.eclipse.jface.text.{TextAttribute, TextPresentation}
import org.eclipse.jface.text.rules.{DefaultDamagerRepairer, ITokenScanner}
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyleRange


/**
 * An extension of default damager/repairer that allows extended text styles
 * via ExtendedTextAttribute.
 *
 * @author Andrius Velykis
 */
class ExtendedStylesDamagerRepairer(scanner: ITokenScanner)
    extends DefaultDamagerRepairer(scanner) {

  override def addRange(presentation: TextPresentation,
                        offset: Int,
                        length: Int,
                        attr: TextAttribute) {

    if (Option(attr).isDefined) {

      val style = attr.getStyle
      val fontStyle = style & (SWT.ITALIC | SWT.BOLD | SWT.NORMAL)

      val styleRange = new StyleRange(
        offset, length, attr.getForeground, attr.getBackground, fontStyle)
      styleRange.strikeout = (style & TextAttribute.STRIKETHROUGH) != 0
      styleRange.underline = (style & TextAttribute.UNDERLINE) != 0
      styleRange.font = attr.getFont

      // if extended text attribute, apply additional styles
      attr match {
        case ext: ExtendedTextAttribute => {
          styleRange.underlineStyle = ext.underlineStyle
        }

        case _ =>
      }

      presentation.addStyleRange(styleRange)
    }
  }

}
