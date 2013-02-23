package isabelle.eclipse.ui.views;

import isabelle.eclipse.ui.IsabelleImages;

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
		return IsabelleImages.RAW_OUTPUT_CONSOLE();
	}



}
