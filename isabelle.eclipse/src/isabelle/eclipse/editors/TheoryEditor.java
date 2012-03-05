package isabelle.eclipse.editors;

import java.io.File;
import java.net.URI;
import isabelle.Document.Snapshot;
import isabelle.Session;
import isabelle.Thy_Header;
import isabelle.eclipse.IsabelleEclipsePlugin;
import isabelle.eclipse.core.IsabelleCorePlugin;
import isabelle.eclipse.core.app.IIsabelleSessionListener;
import isabelle.eclipse.core.app.Isabelle;
import isabelle.eclipse.views.TheoryOutlinePage;
import isabelle.scala.DocumentRef;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.editors.text.ILocationProviderExtension;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import scala.Option;


public class TheoryEditor extends TextEditor {

	public static final String EDITOR_ID = "isabelle.eclipse.editors.TheoryEditor";
	public static final String EDITOR_SCOPE = "isabelle.eclipse.editor.theoryEditorScope";
	
	private ColorManager colorManager;
	
	private final IIsabelleSessionListener sessionListener;
	
	private Session isabelleSession;
	
	private DocumentRef documentRef;
	private DocumentModel isabelleModel;
	
	private TheoryOutlinePage outlinePage = null;
	
	private final TheoryAnnotations markers = new TheoryAnnotations(this);
	
	private int submittedOffset = 0;
	
	public TheoryEditor() {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new IsabelleTheoryConfiguration(this, colorManager));
//		setSourceViewerConfiguration(new TextSourceViewerConfiguration(EditorsUI.getPreferenceStore()));
		setDocumentProvider(new IsabelleFileDocumentProvider());
		
		Isabelle isabelle = IsabelleCorePlugin.getIsabelle();
		
		isabelle.addSessionListener(sessionListener = new IIsabelleSessionListener() {
			
			@Override
			public void systemInit() {
				initIsabelleSystem();
			}

			@Override
			public void sessionInit(Session session) {
				initSession(session, getEditorInput());
			}
			
			@Override
			public void sessionShutdown(Session session) {
				shutdownSession(session, false);
			}
		});
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		
		// init Isabelle from core plugin
		Isabelle isabelle = IsabelleCorePlugin.getIsabelle();
		Session session = isabelle.getSession();
		if (session != null) {
			initSession(session, input);
		}
		
		super.init(site, input);
		
		activateContext();
	}
	
	/**
	 * Activate a context that this view uses. It will be tied to this editor
	 * activation events and will be removed when the editor is disposed.
	 */
	private void activateContext() {
		IContextService service = (IContextService) getSite().getService(IContextService.class);
		service.activateContext(EDITOR_SCOPE);
	}

	private void initIsabelleSystem() {
		
		if (getEditorInput() != null) {
			// reload editor
			// TODO ask for save?
			reload();
		}
	}

	private void reload() {
		// reload input in the UI thread
		UIJob reloadJob = new UIJob("Reloading editor") {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				setInput(getEditorInput());
				return Status.OK_STATUS;
			}
		};
		reloadJob.schedule();
	}
	
	private void reloadOutline() {
		if (outlinePage == null) {
			return;
		}
		
		// reload outline in the UI thread
		UIJob reloadJob = new UIJob("Reloading outline") {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				outlinePage.reload();
				return Status.OK_STATUS;
			}
		};
		reloadJob.schedule();
	}

	@Override
	public void dispose() {
		
		if (isabelleSession != null) {
			shutdownSession(isabelleSession, true);
		}
		
		// TODO review what happens if a second editor is opened for the same input
		Isabelle isabelle = IsabelleCorePlugin.getIsabelle();
		isabelle.removeSessionListener(sessionListener);
		
		markers.dispose();
		
		colorManager.dispose();
		super.dispose();
	}
	
	private void initSession(Session session, IEditorInput input) {
		
		Assert.isTrue(this.isabelleSession == null);
		
		this.isabelleSession = session;
		initIsabelleModel();
		
		reloadOutline();
	}

	private void shutdownSession(Session session, boolean dispose) {
		
		Assert.isTrue(this.isabelleSession == session);
		
		disposeIsabelleModel();
		this.isabelleSession = null;
		
		if (!dispose) {
			// not disposing, just shutting down the session
			reloadOutline();
		}
	}
	
	@Override
	protected void doSetInput(IEditorInput input) throws CoreException {
		
		// disconnect old model
		disposeIsabelleModel();
		
		super.doSetInput(input);
		
		this.documentRef = createDocumentRef(input);
		
		// connect to the new document
		initIsabelleModel();
		
		markers.updateAllMarkers();
		
		// TODO need to reload?
		reloadOutline();
	}
	
	private void initIsabelleModel() {
		IDocument document = getDocument();
		if (document != null && documentRef != null && isabelleSession != null) {
			isabelleModel = DocumentModel.create(isabelleSession, document, documentRef);
			isabelleModel.setSubmitOffset(submittedOffset);
		}
	}
	
	private void disposeIsabelleModel() {
		if (isabelleModel != null) {
			isabelleModel.dispose();
		}
		isabelleModel = null;
	}

	public int getCaretPosition() {
		return getSourceViewer().getTextWidget().getCaretOffset();
	}
	
	public Snapshot getSnapshot() {
		
		if (isabelleModel == null) {
			return null;
		}
		
		return isabelleModel.getSnapshot();
	}
	
	private static String getPath(IEditorInput input) {
		
		if (input == null) {
			return null;
		}
		
		IFile file= (IFile) input.getAdapter(IFile.class);
		if (file != null) {
			IPath location= file.getLocation();
			if (location != null) {
				return location.toOSString();
			}
		} else {
			ILocationProvider provider= (ILocationProvider) input.getAdapter(ILocationProvider.class);
			if (provider instanceof ILocationProviderExtension) {
				URI uri= ((ILocationProviderExtension)provider).getURI(input);
				if (ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(uri).length == 0) {
					try {
						IFileStore fileStore= EFS.getStore(uri);
						File jfile = fileStore.toLocalFile(EFS.NONE, null);
						return jfile.getAbsolutePath();
					} catch (CoreException ex) {
						// TODO ignore?
					}
				}
			}
			if (provider != null) {
				IPath location= provider.getPath(input);
				if (location != null) {
					return location.toOSString();
				}
			}
		}
		
		return null;
	}
	
	private static DocumentRef createDocumentRef(IEditorInput input) {
		Assert.isNotNull(input);
		
		String path = getPath(input);
		if (path == null) {
			return null;
		}
		
		Option<String> theoryNameOpt = Thy_Header.thy_name(path);
		if (theoryNameOpt.isEmpty()) {
			// TODO some warning that the input is not a theory file?
			return null;
		}
		
		String theoryName = theoryNameOpt.get();
		
		return DocumentRef.create(path, new File(path).getParent(), theoryName);
	}
	
	public Session getIsabelleSession() {
		return isabelleSession;
	}
	
	public DocumentModel getIsabelleModel() {
		return isabelleModel;
	}
	
	public void submitToCaret() {
		
		if (isabelleModel == null) {
			// TODO show a message that model is not available?
			System.out.println("Isabelle model is not available for " + getPath(getEditorInput()));
			return;
		}
		
		int caretOffset = getCaretPosition();
		
		IDocument document = getDocument();
		
		try {
			
			int caretLine = document.getLineOfOffset(caretOffset);
			int caretLineEnd = getLineEndOffset(document, caretLine);
			
			System.out.println("Go to Command: " + getTitle() + ":[" + caretLine + "," + caretLineEnd + "]");
			
			isabelleModel.setSubmitOffset(caretLineEnd);
			isabelleModel.updatePerspective();
			
			this.submittedOffset = caretLineEnd;
			
			// TODO review
			reloadOutline();
			
		} catch (BadLocationException ex) {
			IsabelleEclipsePlugin.log("Bad location in the document", ex);
		}
	}
	
	private static int getLineEndOffset(IDocument document, int line) throws BadLocationException {
		return document.getLineOffset(line) + document.getLineLength(line);
	}
	
	public IDocument getDocument() {
		IDocumentProvider provider = getDocumentProvider();
		if (provider == null) {
			return null;
		}
		
		return provider.getDocument(getEditorInput());
	}

	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		if (IContentOutlinePage.class.equals(adapter)) {
			if (outlinePage == null) {
				outlinePage = new TheoryOutlinePage(this);
			}
			return outlinePage;
		}
		return super.getAdapter(adapter);
	}
	
}
