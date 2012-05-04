package isabelle.eclipse.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;

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
	
	/**
	 * Create the Editor Input appropriate for the given <code>IFileStore</code>.
	 * The result is a normal file editor input if the file exists in the
	 * workspace and, if not, we create a wrapper capable of managing an
	 * 'external' file using its <code>IFileStore</code>.
	 * 
	 * @param fileStore
	 *            The file store to provide the editor input for
	 * @return The editor input associated with the given file store
	 * @since 3.3
	 * 
	 * Copied from {@link org.eclipse.ui.ide.IDE#getEditorInput(IFileStore)}
	 */
	public static IEditorInput getEditorInput(IFileStore fileStore) {
		IFile workspaceFile = getWorkspaceFile(fileStore);
		if (workspaceFile != null)
			return new FileEditorInput(workspaceFile);
		return new FileStoreEditorInput(fileStore);
	}

	/**
	 * Determine whether or not the <code>IFileStore</code> represents a file
	 * currently in the workspace.
	 * 
	 * @param fileStore
	 *            The <code>IFileStore</code> to test
	 * @return The workspace's <code>IFile</code> if it exists or
	 *         <code>null</code> if not
	 *         
	 * Copied from {@link org.eclipse.ui.ide.IDE#getWorkspaceFile(IFileStore)}
	 */
	private static IFile getWorkspaceFile(IFileStore fileStore) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IFile[] files = root.findFilesForLocationURI(fileStore.toURI());
		files = filterNonExistentFiles(files);
		if (files == null || files.length == 0)
			return null;

		// for now only return the first file
		return files[0];
	}

	/**
	 * Filter the incoming array of <code>IFile</code> elements by removing
	 * any that do not currently exist in the workspace.
	 * 
	 * @param files
	 *            The array of <code>IFile</code> elements
	 * @return The filtered array
	 * 
	 * Copied from {@link org.eclipse.ui.ide.IDE#filterNonExistentFiles(IFile[])}
	 */
	private static IFile[] filterNonExistentFiles(IFile[] files) {
		if (files == null)
			return null;

		int length = files.length;
		ArrayList<IFile> existentFiles = new ArrayList<IFile>(length);
		for (int i = 0; i < length; i++) {
			if (files[i].exists())
				existentFiles.add(files[i]);
		}
		return existentFiles.toArray(new IFile[existentFiles.size()]);
	}

}
