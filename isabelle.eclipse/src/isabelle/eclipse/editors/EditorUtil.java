package isabelle.eclipse.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class EditorUtil {

	/**
	 * Retrieves all open editors in the workbench.
	 * 
	 * @return
	 */
	public static List<IEditorPart> getOpenEditors() {
		List<IEditorPart> editors = new ArrayList<IEditorPart>();
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (IWorkbenchPage page : window.getPages()) {
				for (IEditorReference editor : page.getEditorReferences()) {
					IEditorPart editorPart = editor.getEditor(false);
					if (editorPart != null) {
						// editors can be null if there are problems
						// instantiating them
						editors.add(editorPart);
					}
				}
			}
		}

		return editors;
	}

}
