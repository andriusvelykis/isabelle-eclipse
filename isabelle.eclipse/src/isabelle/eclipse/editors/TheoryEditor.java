package isabelle.eclipse.editors;

import java.io.File;
import java.net.URI;
import isabelle.Command;
import isabelle.eclipse.IsabelleEclipsePlugin;
import isabelle.eclipse.core.IsabelleCorePlugin;
import isabelle.eclipse.core.app.IIsabelleSessionListener;
import isabelle.eclipse.core.app.IIsabelleSystemListener;
import isabelle.eclipse.core.app.Isabelle;
import isabelle.eclipse.views.TheoryOutlinePage;
import isabelle.scala.DocumentModel;
import isabelle.scala.IsabelleSystemFacade;
import isabelle.scala.SessionFacade;
import isabelle.scala.SnapshotFacade;
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
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
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


public class TheoryEditor extends TextEditor {

	public static final String EDITOR_ID = "isabelle.eclipse.editors.TheoryEditor";
	public static final String EDITOR_SCOPE = "isabelle.eclipse.editor.theoryEditorScope";
	
	private ColorManager colorManager;
	
	private final IIsabelleSystemListener systemListener;
	private IsabelleSystemFacade isabelleSystem;
	
	private final IIsabelleSessionListener sessionListener;
//	private SessionFacade isabelleSession;
	
	private DocumentModel isabelleModel;
	private final DocumentModelManager isabelleModelManager = new DocumentModelManager();
	private final FlushJob flushJob = new FlushJob();
	
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
		isabelle.addSystemListener(systemListener = new IIsabelleSystemListener() {

			@Override
			public void systemInit(IsabelleSystemFacade system) {
				initIsabelleSystem(system);
			}
			
			@Override
			public void systemShutdown(IsabelleSystemFacade system) {
				shutdownIsabelle(system);
			}
		});
		
		isabelle.addSessionListener(sessionListener = new IIsabelleSessionListener() {

			@Override
			public void sessionInit(SessionFacade session) {
				initSession(session, getEditorInput());
			}
			
			@Override
			public void sessionShutdown(SessionFacade session) {
				shutdownSession(session);
			}
		});
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		
		// init Isabelle from core plugin
		Isabelle isabelle = IsabelleCorePlugin.getIsabelle();
		initIsabelleSystem(isabelle.getSystem());
		if (isabelleSystem != null) {
			initSession(isabelle.getSession(), input);
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

	private void initIsabelleSystem(IsabelleSystemFacade system) {
		
		Assert.isTrue(this.isabelleSystem == null || this.isabelleSystem == system);
		
		if (this.isabelleSystem == system) {
			// already init
			return;
		}
		
		this.isabelleSystem = system;
		
		if (getEditorInput() != null) {
			// reload editor
			// TODO ask for save?
			reload();
		}
	}

	private void shutdownIsabelle(IsabelleSystemFacade system) {
		
		Assert.isTrue(this.isabelleSystem == system);
		
		// also shutdown the session - cannot have a session running without the system
		if (isabelleModel != null) {
			shutdownSession(isabelleModel.getSession());
		}

		isabelleSystem = null;
		
		// reload editor
		// TODO ask for save?
		reload();
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
		// TODO review what happens if a second editor is opened for the same input
		Isabelle isabelle = IsabelleCorePlugin.getIsabelle();
		isabelle.removeSessionListener(sessionListener);
		isabelle.removeSystemListener(systemListener);
		
		colorManager.dispose();
		super.dispose();
	}
	
	private void initSession(SessionFacade session, IEditorInput input) {
		
		Assert.isTrue(this.isabelleSystem != null 
				&& this.isabelleModel == null || this.isabelleModel.getSession() == session);

		if (this.isabelleModel != null && this.isabelleModel.getSession() == session) {
			// already init
			return;
		}
		
		this.isabelleModel = new DocumentModel(session, getTheoryName(isabelleSystem, input));
//		this.isabelleModel.init(getInitText());
		
		markers.connect(session);
		
		reloadOutline();
	}

	private String getInitText() {
		try {
			IDocument document = getDocument();
			if (document != null) {
				return getDocument().get(0, submittedOffset);
			}
		} catch (BadLocationException ex) {
			// ignore?
		}
		
		return "";
	}
	
	private void shutdownSession(SessionFacade session) {
		
		Assert.isTrue(this.isabelleModel == null || this.isabelleModel.getSession() == session);
		
		markers.disconnect(session);
		
		this.isabelleModel = null;
		
		reloadOutline();
	}
	
	@Override
	protected void doSetInput(IEditorInput input) throws CoreException {
		
		// disconnect from old document
		IDocument oldDocument = getDocument();
		if (oldDocument != null) {
			getDocument().removeDocumentListener(isabelleModelManager);
			// flush if there is something
			flushJob.schedule();
		}
		
		super.doSetInput(input);
		
		// connect to the new document
		IDocument document = getDocument();
		if (document != null) {
			document.addDocumentListener(isabelleModelManager);
			if (isabelleModel != null) {
				String initText = getInitText();
				if (initText.length() > 0) {
					isabelleModel.insertText(0, " ");
					flushJob.schedule();
				}
//				
//				
//				UIJob initJob = new UIJob("Sending Initial Document to Prover") {
//					
//					@Override
//					public IStatus runInUIThread(IProgressMonitor monitor) {
//						isabelleModel.init(getInitText());
//						return Status.OK_STATUS;
//					}
//				};
//				initJob.schedule();
			}
			
			reloadOutline();
		}
		
	}

	public int getCaretPosition() {
		return getSourceViewer().getTextWidget().getCaretOffset();
	}
	
	public Command getSelectedCommand() {
		SnapshotFacade snapshot = getSnapshot();
		if (snapshot == null) {
			return null;
		}
		
		return snapshot.properCommandAt(getCaretPosition());
	}
	
	public SnapshotFacade getSnapshot() {
		
		if (isabelleModel == null) {
			return null;
		}
		
		return new SnapshotFacade(isabelleModel.snapshot());
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
	
	public IsabelleSystemFacade getIsabelle() {
		return isabelleSystem;
	}
	
	public SessionFacade getIsabelleSession() {
		if (isabelleModel == null) {
			return null;
		}
		
		return isabelleModel.getSession();
	}
	
	private static String getTheoryName(IsabelleSystemFacade isabelleSystem, IEditorInput input) {
		
		Assert.isNotNull(isabelleSystem);
		Assert.isNotNull(input);
		
		String path = getPath(input);
		if (path == null) {
			return null;
		}
		
		return isabelleSystem.getThyName(path);
	}
	
	public void submitToCaret() {
		
		int caretOffset = getCaretPosition();
		
		IDocument document = getDocument();
		
		try {
			
			int caretLine = document.getLineOfOffset(caretOffset);
			int caretLineEnd = getLineEndOffset(document, caretLine);
			
			System.out.println("Go to Command: " + getTitle() + ":[" + caretLine + "," + caretLineEnd + "]");

			if (isabelleModel != null) {
				
				if (caretLineEnd > submittedOffset) {
					String insertText = document.get(submittedOffset, caretLineEnd - submittedOffset);
					isabelleModel.insertText(submittedOffset, insertText);
//					System.out.println("Inserting text:\n" + insertText);
				} else if (submittedOffset > caretLineEnd) {
					String removeText = document.get(caretLineEnd, submittedOffset - caretLineEnd);
					isabelleModel.removeText(caretLineEnd, removeText);
//					System.out.println("Removing text:\n" + removeText);
				}
				
				flushJob.schedule();
//				isabelleModel.flush();
			}
			
			this.submittedOffset = caretLineEnd;
			
//			isabelleSession.edit(theoryName, textToLineEnd);
//			reload();
			
			// TODO review
			reloadOutline();
			
			System.out.println("Done editing");
			
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
	
	private void flushDelayed() {
		flushJob.cancel();
		
		long delay = 300;
		if (isabelleModel != null) {
			delay = isabelleModel.getSession().getInputDelay();
		}
		
		flushJob.schedule(delay);
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

	private class DocumentModelManager implements IDocumentListener {
		
		@Override
		public void documentChanged(DocumentEvent event) {
			
			if (isabelleModel == null) {
				// isabelle model not yet initialised - do not listen to changes
				return;
			}
			
			String text = event.getText();
			if (text != null && text.length() > 0) {
				// inserted
				
				if (event.getOffset() > submittedOffset) {
					// the change is after the submitted offset - ignore this change
					return;
				}
				
				// update submitted line to incorporate added lines
				submittedOffset = submittedOffset + text.length();
				System.out.println("New submitted offset ins: " + submittedOffset);
				
				isabelleModel.insertText(event.getOffset(), text);
				flushDelayed();
			}
		}

		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
			
			if (isabelleModel == null) {
				// isabelle model not yet initialised - do not listen to changes
				return;
			}

			if (event.getLength() > 0) {
				// removed
				IDocument document = event.getDocument();
				try {
					
					if (event.getOffset() > submittedOffset) {
						// the change is after the submitted offset - ignore this change
						return;
					}

					// update submitted line to incorporate removed lines
					
					int eventLength = event.getLength();
					if (event.getOffset() + eventLength > submittedOffset) {
						// deleting after submitted offset as well - only remove up to submitted offset from the document
						eventLength = submittedOffset - event.getOffset();
					}
					
					String text = document.get(event.getOffset(), eventLength);
					
					// deleted before offset - move by the removed amount
					submittedOffset = submittedOffset - text.length();

					System.out.println("New submitted offset rem: " + submittedOffset);
					
					isabelleModel.removeText(event.getOffset(), text);
					flushDelayed();
				} catch (BadLocationException ex) {
					// ignore
				}
			}
		}

	}
	
	private class FlushJob extends Job {
//	private class FlushJob extends UIJob {

		public FlushJob() {
			super("Sending Changes to Prover");
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
//		@Override
//		public IStatus runInUIThread(IProgressMonitor monitor) {
			if (isabelleModel != null) {
				System.out.println("Flushing");
				isabelleModel.flush();
			}
			
			return Status.OK_STATUS;
		}
		
	}
	
}
