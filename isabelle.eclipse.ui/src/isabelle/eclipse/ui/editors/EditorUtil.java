package isabelle.eclipse.ui.editors;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

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
	
	/**
	 * Finds a corresponding workspace resource for the given element (e.g. editor input).
	 * 
	 * @param element
	 *            The element for which to resolve resource (e.g. an editor input)
	 * @return A resource corresponding to the given element, or {@code null}
	 */
	public static IResource getResource(Object element) {
		// try resolving as file (a number of options there)
		IResource resource = ResourceUtil.getFile(element);
		if (resource == null) {
			// try at least resource
			resource = ResourceUtil.getResource(element);
		}

		return resource;
	}
	
	/**
	 * Retrieves a {@link ITextViewer} for a given editor. Assumes that editor's
	 * {@link ITextOperationTarget} is the text viewer and resolves it via the adapter.
	 * 
	 * @param editor
	 * @return the text viewer, {@code null} if editor's {@link ITextOperationTarget} is not a text
	 *         viewer.
	 * 
	 * @see <a href="http://stackoverflow.com/questions/923342/get-itextviewer-from-ieditorpart-eclipse">From StackOverflow</a>
	 */
	public static ITextViewer getTextViewer(IEditorPart editor) {
		ITextOperationTarget target = (ITextOperationTarget) editor.getAdapter(ITextOperationTarget.class);
		if (target instanceof ITextViewer) {
			return (ITextViewer) target;
		}

		return null;
	}
	
	/**
	 * Retrieves the text editor's document via its document provider.
	 * 
	 * @param editor
	 * @return the document, or {@code null} if none or provider is unavailable
	 */
	public static IDocument getDocument(ITextEditor editor) {
		IDocumentProvider provider = editor.getDocumentProvider();
		if (provider != null) {
			return provider.getDocument(editor.getEditorInput());
		}
		
		return null;
	}
	
	/**
	 * Selects and reveals the given region in the given editor part.
	 *
	 * @param part the editor part
	 * @param element the Java element to reveal
	 * 
	 * @see From org.eclipse.jdt.internal.ui.javaeditor.EditorUtility
	 */
	public static void revealInEditor(IEditorPart part, IRegion selectRegion, IRegion highlightRegion) {
		if (part == null || selectRegion == null || highlightRegion == null) {
			return;
		}
		
		if (part instanceof TheoryEditor) {
			((TheoryEditor) part).selectInEditor(selectRegion, highlightRegion);
		} else {
			revealInEditor(part, selectRegion.getOffset(), selectRegion.getLength(),
					highlightRegion.getOffset(), highlightRegion.getLength());
		}
	}

	/**
	 * Selects and reveals the given offset and length in the given editor part.
	 * @param editor the editor part
	 * @param offset the offset
	 * @param length the length
	 * @param highlightOffset the highlight offset
	 * @param highlightLength the highlight length
	 * 
	 * @see From org.eclipse.jdt.internal.ui.javaeditor.EditorUtility
	 */
	public static void revealInEditor(IEditorPart editor, final int offset, final int length, 
			int highlightOffset, int highlightLength) {
		
		if (editor instanceof ITextEditor) {
			((ITextEditor)editor).setHighlightRange(highlightOffset, highlightLength, true);
			((ITextEditor)editor).selectAndReveal(offset, length);
			return;
		}

		// Support for non-text editor - try IGotoMarker interface
		final IGotoMarker gotoMarkerTarget;
		if (editor instanceof IGotoMarker)
			gotoMarkerTarget= (IGotoMarker)editor;
		else
			gotoMarkerTarget= editor != null ? (IGotoMarker)editor.getAdapter(IGotoMarker.class) : null;
		if (gotoMarkerTarget != null) {
			final IEditorInput input= editor.getEditorInput();
			if (input instanceof IFileEditorInput) {
				WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
					@Override
					protected void execute(IProgressMonitor monitor) throws CoreException {
						IMarker marker= null;
						try {
							marker= ((IFileEditorInput)input).getFile().createMarker(IMarker.TEXT);
							marker.setAttribute(IMarker.CHAR_START, offset);
							marker.setAttribute(IMarker.CHAR_END, offset + length);

							gotoMarkerTarget.gotoMarker(marker);

						} finally {
							if (marker != null)
								marker.delete();
						}
					}
				};

				try {
					op.run(null);
				} catch (InvocationTargetException ex) {
					// reveal failed
				} catch (InterruptedException e) {
					Assert.isTrue(false, "this operation can not be canceled"); //$NON-NLS-1$
				}
			}
			return;
		}

		/*
		 * Workaround: send out a text selection
		 * XXX: Needs to be improved, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=32214
		 */
		if (editor != null && editor.getEditorSite().getSelectionProvider() != null) {
			IEditorSite site= editor.getEditorSite();
			if (site == null)
				return;

			ISelectionProvider provider= editor.getEditorSite().getSelectionProvider();
			if (provider == null)
				return;

			provider.setSelection(new TextSelection(offset, length));
		}
	}
	
	/**
	 * Opens an editor on the given IFileStore object.
	 * <p>
	 * This method can be used to open files that reside outside the workspace resource set.
	 * </p>
	 * <p>
	 * If the page already has an editor open on the target object then that editor is brought to
	 * front; otherwise, a new editor is opened.
	 * </p>
	 * 
	 * @param page
	 *            the page in which the editor will be opened
	 * @param uri
	 *            the URI of the file store representing the file to open
	 * @return an open editor or {@code null} if an external editor was opened
	 * @exception PartInitException
	 *                if the editor could not be initialized
	 * 
	 * @see org.eclipse.ui.ide.IDE#openEditorOnFileStore(IWorkbenchPage, IFileStore)
	 * @see EFS#getStore(URI)
	 * 
	 * @since 3.3
	 */
	public static IEditorPart openEditor(IWorkbenchPage page, URI uri) throws PartInitException {
		// sanity checks
		if (page == null) {
			throw new IllegalArgumentException();
		}

		IFileStore fileStore;
		try {
			fileStore = EFS.getStore(uri);
		} catch (CoreException e) {
			throw new PartInitException("CoreException opening the file store on the URI.", e);
		}

		// open the editor on the file
		return IDE.openEditorOnFileStore(page, fileStore);
	}
	
	/**
	 * Retrieves active page from the workbench.
	 * 
	 * @return the active page or {@code null} if not available or called from non-UI thread
	 */
	public static IWorkbenchPage getActivePage() {
		IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (activeWindow != null) {
			return activeWindow.getActivePage();
		}
		
		return null;
	}

}
