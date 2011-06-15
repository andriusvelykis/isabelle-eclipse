package isabelle.eclipse.actions;

import isabelle.eclipse.editors.TheoryEditor;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

public class SubmitToPointCommand extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		TheoryEditor editor = (TheoryEditor) HandlerUtil.getActiveEditor(event);
		editor.submitToCaret();
		
		return null;
	}

}
