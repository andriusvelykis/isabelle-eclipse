package isabelle.eclipse.ui.editors

import org.eclipse.jface.text.TextAttribute
import org.eclipse.swt.graphics.{Color, Font}
import org.eclipse.swt.SWT


/**
 * An extended text attribute for further editor customisations.
 * 
 * Intended to be used with ExtendedStylesDamagerRepairer
 * 
 * @author Andrius Velykis
 */
class ExtendedTextAttribute(foreground: Color,
                            background: Color,
                            style: Int,
                            font: Font,
                            val underlineStyle: Int = SWT.UNDERLINE_SINGLE)
    extends TextAttribute(foreground, background, style, font) {

}
