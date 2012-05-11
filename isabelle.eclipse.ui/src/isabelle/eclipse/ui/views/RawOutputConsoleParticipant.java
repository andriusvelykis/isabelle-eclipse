package isabelle.eclipse.ui.views;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.console.actions.CloseConsoleAction;
import org.eclipse.ui.part.IPageBookViewPage;

public class RawOutputConsoleParticipant implements IConsolePageParticipant {

	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		System.out.println("Requesting adapter: " + adapter);
		return null;
	}

	@Override
	public void init(IPageBookViewPage page, IConsole console) {
		
		CloseConsoleAction action = new CloseConsoleAction(console);
		IToolBarManager manager = page.getSite().getActionBars().getToolBarManager();
		manager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, action);

	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void activated() {
		// TODO Auto-generated method stub

	}

	@Override
	public void deactivated() {
		// TODO Auto-generated method stub

	}

}
