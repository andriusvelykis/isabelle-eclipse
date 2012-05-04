package isabelle.eclipse.editors;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;

import scala.Option;

import isabelle.Document.Snapshot;
import isabelle.Exn;
import isabelle.Exn.Res;
import isabelle.Exn.Result;
import isabelle.Session;
import isabelle.Text$Edit$;
import isabelle.Text$Perspective$;
import isabelle.Text.Edit;
import isabelle.Text.Perspective;
import isabelle.Text.Range;
import isabelle.Thy_Header;
import isabelle.eclipse.IsabelleEclipsePlugin;
import isabelle.scala.DocumentRef;
import isabelle.scala.ScalaCollections;

public class DocumentModel {

	private static final Text$Edit$ TEXT_EDIT = Text$Edit$.MODULE$;
	
	private final Session session;
	private final IDocument document;
	private final DocumentRef name;
	
	private final IDocumentListener documentListener;
	
	private final List<Edit> pendingEdits = new LinkedList<Edit>();
	private boolean pendingPerspective = false;
	private Perspective lastPerspective = Text$Perspective$.MODULE$.empty();
	
	private int submitOffset = 0;
	private Range activePerspectiveRange = new Range(0, 0);
	
	private ReadWriteLock pendingEditsLock = new ReentrantReadWriteLock();
	
	private Job flushJob = new FlushJob(); 
	
	private DocumentModel(Session session, IDocument document, DocumentRef name) {
		super();
		this.session = session;
		this.document = document;
		this.name = name;
		
		this.documentListener = new IDocumentListener() {
			
			@Override
			public void documentChanged(DocumentEvent event) {
				// do the inserts after the change
				if (!event.getText().isEmpty()) {
					
					if (event.getOffset() <= submitOffset) {
						// update the offset with inserted
						submitOffset = submitOffset + event.getText().length();
					}
					
					// something's inserted
					addPendingEdit(TEXT_EDIT.insert(event.getOffset(), event.getText()));
				}
			}
			
			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
				// do the removals before the change
				if (event.getLength() > 0) {
					/*
					 * Replaced text is non empty - something was removed.
					 * Note, if a replacement is going on, it will be caught in #documentChanged(),
					 * and an additional insert will be added. So a replacement will be represented
					 * as Remove-Insert in Isabelle.
					 */
					String removedText;
					try {
						 removedText = event.getDocument().get(event.getOffset(), event.getLength());
					} catch (BadLocationException e) {
						// the event is badly constructed?
						throw new RuntimeException(e);
					}
					
					if (event.getOffset() <= submitOffset) {
						// update the offset with removed
						submitOffset = submitOffset - event.getLength();
					}
					
					addPendingEdit(TEXT_EDIT.remove(event.getOffset(), removedText));
				}
			}
		};
	}
	
	private Result<Thy_Header> parseTheoryHeader() {
		try {
			Thy_Header result = Thy_Header.check(name.getTheory(), document.get());//.replace("\r\n", "\n"));
			return new Res<Thy_Header>(result);
		} catch (Throwable ex) {
			return new Exn.Exn<Thy_Header>(ex);
		}
	}
	
	private void addPendingEdit(Edit edit) {
		Lock writeLock = pendingEditsLock.writeLock();
		writeLock.lock();
		
		pendingEdits.add(edit);
		
		writeLock.unlock();
		
		flushDelayed();
	}
	
	private void flushDelayed() {
		flush(session.input_delay().ms());
	}
	
	private void flush(long delay) {
		Job flushJob = new FlushJob();
		
		// cancel the previous job
		this.flushJob.cancel();
		this.flushJob = flushJob;
		
		this.flushJob.schedule(delay);
	}
	
	public void setActivePerspective(int offset, int length) {
		Range docRange = new Range(0, document.getLength());
		Range newRange = new Range(offset, offset + length);
		
		Option<Range> fixedRangeOpt = docRange.try_restrict(newRange);
		if (fixedRangeOpt.isEmpty()) {
			// bad range (outside the document?)
			return;
		}
		
		Range fixedRange = fixedRangeOpt.get();
		
		Range previousRange = activePerspectiveRange;
		activePerspectiveRange = fixedRange;
		
		if (!activePerspectiveRange.equals(previousRange)) {
			updatePerspective();
		}
	}
	
	private Perspective getCurrentPerspective() {
	
		// TODO also allow explicitly saying how much to calculate - e.g. with a submitOffset?
		return new Perspective(ScalaCollections.singletonList(activePerspectiveRange));
	}
	
	public void setSubmitOffset(int offset) {
		int newOffset = Math.max(offset, 0);
		if (this.submitOffset != newOffset) {
			this.submitOffset = newOffset;
			updatePerspective();
		}
	}
	
	public Range getDocumentRange() {
		return new Range(0, Math.max(document.getLength() - 1, 0));
	}
	
	public void updatePerspective() {
		pendingPerspective = true;
		flushDelayed();
	}
	
	public void submitFullPerspective(IProgressMonitor monitor) {
		
		flushPending(monitor);
		lockSubmit(monitor);
		
		session.edit_node(name.getRef(), parseTheoryHeader(), new Perspective(
				ScalaCollections.singletonList(getDocumentRange())),
				ScalaCollections.<Edit>emptyList());
		
		unlockSubmit();
	}
	
	private void flushPending(IProgressMonitor monitor) {
		
		Lock readLock = pendingEditsLock.readLock();
		readLock.lock();
		
		// copy the edits into an immutable Scala List, which the API expects
		scala.collection.immutable.List<Edit> edits = ScalaCollections.toScalaList(pendingEdits);
		// clear the pending list
		pendingEdits.clear();
		
		readLock.unlock();
		
		Perspective newPerspective;
		if (pendingPerspective) {
			pendingPerspective = false;
			newPerspective = getCurrentPerspective();
		} else {
			newPerspective = lastPerspective;
		}
		
		if (edits.isEmpty() && newPerspective.equals(lastPerspective)) {
			// no edits and the same perspective - nothing to flush
			return;
		}
		
		lastPerspective = newPerspective;
		
		lockSubmit(monitor);
		session.edit_node(name.getRef(), parseTheoryHeader(), newPerspective, edits);
		unlockSubmit();
	}
	
	private void init() {
		
		// start listening on the document
		document.addDocumentListener(documentListener);
		
		// need a lock on the document for initialisation? E.g. to avoid edits while initialising?
		// technically this should come from the SWT thread so should not be any need?
		lockSubmit(new NullProgressMonitor());
		session.init_node(name.getRef(), parseTheoryHeader(), getCurrentPerspective(), document.get());
		unlockSubmit();
	}
	
	public void dispose() {
		document.removeDocumentListener(documentListener);
		
		// cancel possibly delayed job
		flushJob.cancel();
		// force flush
		flushPending(new NullProgressMonitor());
	}
	
	public Snapshot getSnapshot() {
		
		Lock readLock = pendingEditsLock.readLock();
		readLock.lock();

		// copy the edits into an immutable Scala List, which the API expects
		scala.collection.immutable.List<Edit> edits = ScalaCollections.toScalaList(pendingEdits);
		
		readLock.unlock();
		
		// get snapshot of the pending edits, and merge with the Session snapshot
		return session.snapshot(name.getRef(), edits);
	}
	
	public Session getSession() {
		return session;
	}

	public DocumentRef getName() {
		return name;
	}

	/**
	 * Locks the submit to Isabelle. This is used to wrap the submit to Isabelle
	 * into a scheduling rule, enforcing sequential submits. Since the rule can
	 * be nested, it will be ok if called from a flush job already.
	 */
	private void lockSubmit(IProgressMonitor monitor) {
		Job.getJobManager().beginRule(IsabelleEclipsePlugin.ISABELLE_SUBMIT, monitor);
	}
	
	private void unlockSubmit() {
		Job.getJobManager().endRule(IsabelleEclipsePlugin.ISABELLE_SUBMIT);
	}
	
	public static DocumentModel create(Session session, IDocument document, DocumentRef name) {
		DocumentModel model = new DocumentModel(session, document, name);
		model.init();
		return model;
	}
	
	private class FlushJob extends Job {

		public FlushJob() {
			super("Sending Changes to Prover");
			// set Isabelle Submit rule, to enforce sequential submitting to Isabelle
			setRule(IsabelleEclipsePlugin.ISABELLE_SUBMIT);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			
			flushPending(monitor);
			return Status.OK_STATUS;
		}
	}
	
}
