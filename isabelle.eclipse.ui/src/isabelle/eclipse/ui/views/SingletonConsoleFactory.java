package isabelle.eclipse.ui.views;

import java.util.Arrays;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;

public abstract class SingletonConsoleFactory implements IConsoleFactory {

	private IConsole console = null;
	
	@Override
	public void openConsole() {
		
		IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
		
		boolean consoleOpen = console != null && Arrays.asList(consoleManager.getConsoles()).contains(console);
		
		if (!consoleOpen) {
			
			if (console == null) {
				// no console yet, so create a new one
				console = createConsole();
			}
			
			consoleManager.addConsoles(new IConsole[] {console});
		}
		
		// display Console View
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                try {
                	IConsoleView view = (IConsoleView) page.showView(IConsoleConstants.ID_CONSOLE_VIEW);
                	view.display(console);
                } catch (PartInitException e) {
                    ConsolePlugin.log(e);
                }
            }
        }

	}
	
	protected IConsole getConsole() {
		return console;
	}

	protected abstract IConsole createConsole();
	
//	private static MessageConsole findConsole(String name) {
//
//		ConsolePlugin plugin = ConsolePlugin.getDefault();
//		IConsoleManager conMan = plugin.getConsoleManager();
//		IConsole[] existing = conMan.getConsoles();
//		
//		for (IConsole console : existing) {
//			if (name.equals(console.getName())) {
//				return (MessageConsole) console;
//			}
//		}
//		
//		// no console found, so create a new one
//		ImageDescriptor consoleIcon = FrameworkUIImages.getImageDescriptor(FrameworkUIImages.IMG_PROVER_LOG_CONSOLE);
//		MessageConsole proverLogConsole = new MessageConsole(name, consoleIcon);
//		conMan.addConsoles(new IConsole[] { proverLogConsole });
//		return proverLogConsole;
//	}

}
