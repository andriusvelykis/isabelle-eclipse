package isabelle.eclipse.views;

import isabelle.eclipse.IsabelleEclipseImages;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.IConsole;


public class RawOutputConsoleFactory extends SingletonConsoleFactory {
	
	@Override
	protected IConsole createConsole() {
		return new RawOutputConsole(getConsoleName(), getConsoleImage());
	}

	private String getConsoleName() {
		return "Isabelle Raw Output";
	}

	private ImageDescriptor getConsoleImage() {
		return IsabelleEclipseImages.getImageDescriptor(IsabelleEclipseImages.IMG_RAW_OUTPUT_CONSOLE);
	}



}
