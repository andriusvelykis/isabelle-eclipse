package isabelle.eclipse.editors;

import java.net.URI;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorLauncher;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;


public class TheoryEditorLauncher implements IEditorLauncher {
	
	public static final String ISABELLE_EXTERNAL_FILES_PROJECT = "Isabelle External Files";

	private ExternalResourcesProject externalProject = new ExternalResourcesProject();
	
	@Override
	public void open(IPath location) {

		System.out.println("Open request: " + location.toString());
		
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if (page == null) {
			return;
		}
		
		IFile file = getWorkspaceFile(location);
		IEditorInput editorInput;
		if (file != null) {
			editorInput = new FileEditorInput(file);
		} else {
			IFileStore fileStore = EFS.getLocalFileSystem().getStore(location);
			editorInput = new FileStoreEditorInput(fileStore);
		}
		
		try {
			page.openEditor(editorInput, TheoryEditor.EDITOR_ID);
		} catch (PartInitException ex) {
			MessageDialog.openError(PlatformUI.getWorkbench().getDisplay().getActiveShell(),
					"Error", ex.getLocalizedMessage());
		}
	}
	
//	private void openFileStore(IPath file) {
//		IFileStore localFile = EFS.getLocalFileSystem().getStore(file);
//		
//		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
//		
//		IsabelleFileStore isabelleFile = new IsabelleFileStore(localFile, file);
//		// treat as external file to use the customised file store
//		IEditorInput input = new IsabelleEditorInput(isabelleFile);
//		
//		try {
//			page.openEditor(input, TheoryEditor.EDITOR_ID);
//		} catch (PartInitException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	
	private IFile getWorkspaceFile(IPath location) {
		
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		
		URI locationURI = URIUtil.toURI(location.makeAbsolute());
		
		IFile[] found = workspaceRoot.findFilesForLocationURI(locationURI, IWorkspaceRoot.INCLUDE_HIDDEN);
		if (found.length > 0) {
			// take the first one
			return found[0];
		}
		
		return externalProject.getExternalFile(location);
	}

}
