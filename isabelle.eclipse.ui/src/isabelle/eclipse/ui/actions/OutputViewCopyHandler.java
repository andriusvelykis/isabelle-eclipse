package isabelle.eclipse.ui.actions;

import isabelle.eclipse.ui.views.ProverOutputPage;
import isabelle.eclipse.ui.views.ProverOutputView;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.ui.handlers.HandlerUtil;

public class OutputViewCopyHandler extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		ProverOutputView outputView = (ProverOutputView) HandlerUtil.getActivePart(event);
		ProverOutputPage outputPage = (ProverOutputPage) outputView.getCurrentPage();
		if (outputPage == null) {
			return null;
		}
		
		outputPage.copySelectionToClipboard();
		return null;
	}

}
