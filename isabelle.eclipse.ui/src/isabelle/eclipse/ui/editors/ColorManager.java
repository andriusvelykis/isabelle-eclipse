package isabelle.eclipse.ui.editors;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ColorManager implements ISharedTextColors {

	protected Map<RGB, Color> fColorTable = new HashMap<RGB, Color>(10);

	@Override
	public void dispose() {
		for (Color color : fColorTable.values()) {
			color.dispose();
		}
	}
	
	@Override
	public Color getColor(RGB rgb) {
		Color color = fColorTable.get(rgb);
		if (color == null) {
			color = new Color(Display.getCurrent(), rgb);
			fColorTable.put(rgb, color);
		}
		return color;
	}
}
