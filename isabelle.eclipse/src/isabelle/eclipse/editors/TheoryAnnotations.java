package isabelle.eclipse.editors;

import isabelle.Command;
import isabelle.Isar_Document$;
import isabelle.Isar_Document$Unprocessed$;
import isabelle.Document.Snapshot;
import isabelle.Isar_Document.Forked;
import isabelle.Isar_Document;
import isabelle.Markup;
import isabelle.Command.State;
import isabelle.Session;
import isabelle.Session.Commands_Changed;
import isabelle.Text.Info;
import isabelle.Text.Range;
import isabelle.eclipse.IsabelleEclipsePlugin;
import isabelle.eclipse.util.SessionEventSupport;
import isabelle.scala.ISessionCommandsListener;
import isabelle.scala.SessionActor;
import isabelle.scala.SessionEventType;
import isabelle.scala.SnapshotUtil;
import isabelle.scala.SnapshotUtil.MarkupMessage;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;

import scala.Tuple2;
import scala.collection.Iterator;

import static scala.collection.JavaConversions.setAsJavaSet;

public class TheoryAnnotations {
	
	private static final Isar_Document$ ISAR_DOCUMENT = Isar_Document$.MODULE$;
	private static final Isar_Document$Unprocessed$ UNPROCESSED = Isar_Document$Unprocessed$.MODULE$;
	
	private final TheoryEditor editor;

	public static final String MARKER_PROBLEM = "isabelle.eclipse.markerProblem";
	public static final String MARKER_INFO = "isabelle.eclipse.markerInfo";
	// TODO foreground colours, Isabelle_Markup.foreground? Or actually syntax colours?
	public static final String ANNOTATION_BAD = "isabelle.eclipse.editor.markup.bad";
	public static final String ANNOTATION_HILITE = "isabelle.eclipse.editor.markup.hilite";
	public static final String ANNOTATION_TOKEN = "isabelle.eclipse.editor.markup.token";
	
	public static final String ANNOTATION_OUTDATED = "isabelle.eclipse.editor.commandStatus.outdated";
	public static final String ANNOTATION_UNFINISHED = "isabelle.eclipse.editor.commandStatus.unfinished";
	public static final String ANNOTATION_UNPROCESSED = "isabelle.eclipse.editor.commandStatus.unprocessed";
	public static final String ANNOTATION_FAILED = "isabelle.eclipse.editor.commandStatus.failed";
	public static final String ANNOTATION_FINISHED = "isabelle.eclipse.editor.commandStatus.finished";
	
	private static final Object THEORY_ANNOTATIONS = new Object();
	
	private final SessionEventSupport sessionEvents;
	
	private Job updateJob = new AnnotationUpdateJob(Collections.<Command>emptySet());
	
	public TheoryAnnotations(TheoryEditor editor) {
		super();
		this.editor = editor;
		
		sessionEvents = new SessionEventSupport(EnumSet.of(SessionEventType.COMMAND)) {
			
			@Override
			protected SessionActor createSessionActor(Session session) {
				return new SessionActor().commandsChanged(new ISessionCommandsListener() {
					
					@Override
					public void commandsChanged(Commands_Changed changed) {
						// TODO add checks on current node
						updateMarkers(setAsJavaSet(changed.commands()));
					}
				});
			}

			@Override
			protected void sessionInit(Session session) {
				updateAllMarkers();
			}
		};
	}
	
	public void dispose() {
		sessionEvents.dispose();
	}
	
	public void updateAllMarkers() {
		
		DocumentModel isabelleModel = editor.getIsabelleModel();
		Set<Command> snapshotCommands;
		if (isabelleModel != null) {
			snapshotCommands = setAsJavaSet(isabelleModel.getSnapshot().node().commands());
		} else {
			snapshotCommands = Collections.emptySet();
		}
		
		updateMarkers(snapshotCommands);
	}
	
	private void updateMarkers(Set<Command> commands) {
		// TODO add checks on current node
		Job updateJob = new AnnotationUpdateJob(commands);
		
		// cancel the previous job
		this.updateJob.cancel();
		this.updateJob = updateJob;
		
		this.updateJob.schedule();
	}
	
	private AnnotationConfig createMarkers(Set<Command> commands) {
		
		DocumentModel isabelleModel = editor.getIsabelleModel();
		if (isabelleModel == null) {
//			// no model - delete previous markers?
//			deleteMarkers(markerResource);
			return null;
		}
		
		Snapshot snapshot = isabelleModel.getSnapshot();
		
		// copy commands set
		commands = new LinkedHashSet<Command>(commands);
		// only use commands that are in the snapshot
		commands.retainAll(setAsJavaSet(snapshot.node().commands()));

		if (commands.isEmpty()) {
			return null;
		}
		
		AnnotationConfig anns = new AnnotationConfig();
		
		try {

			updateAnnotations(anns, isabelleModel.getSession(), snapshot);
			createMarkupMarkers(anns, snapshot);
		
		} catch (CoreException e) {
			IsabelleEclipsePlugin.log(e.getLocalizedMessage(), e);
		}
		
		return anns;
	}
	
	private void setMarkers(final AnnotationConfig anns) {
		
		if (anns == null) {
			// nothing to update
			return;
		}
		
		final IResource markerResource = getMarkerResource();
		// delete old markers
		deleteMarkers(markerResource);
		
		// replace annotations
		IAnnotationModel baseAnnotationModel = 
				editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
		if (baseAnnotationModel == null) {
			return;
		}

		// use modern models
		Assert.isTrue(baseAnnotationModel instanceof IAnnotationModelExtension);

		AnnotationModel annotationModel = getAnnotationModel(
				(IAnnotationModelExtension) baseAnnotationModel,
				THEORY_ANNOTATIONS);

		List<Annotation> existingAnns = iteratorToList(
				(java.util.Iterator<Annotation>) annotationModel.getAnnotationIterator());
		Annotation[] existingAnnsArray = existingAnns.toArray(new Annotation[existingAnns.size()]);
		annotationModel.replaceAnnotations(existingAnnsArray, anns.annotations);
		
		// add new markers
		IWorkspaceRunnable r = new IWorkspaceRunnable() {
			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				
				for (Entry<String, Map<String, Object>> markerInfo : anns.markers) {
					IMarker marker = markerResource.createMarker(markerInfo.getKey());
					marker.setAttributes(markerInfo.getValue());
				}
			}
		};

		try {
			markerResource.getWorkspace().run(r, null, IWorkspace.AVOID_UPDATE, null);
		} catch (CoreException ce) {
			IsabelleEclipsePlugin.log(ce.getMessage(), ce);
		}
	}
	
	private <T> List<T> iteratorToList(java.util.Iterator<T> it) {
		List<T> list = new ArrayList<T>();
		
		while (it.hasNext()) {
			list.add(it.next());
		}
		
		return list;
	}
	
	private void deleteMarkers(IResource markerResource) {
		// remove all current markers
		try {
			markerResource.deleteMarkers(MARKER_PROBLEM, false, IResource.DEPTH_ZERO);
			markerResource.deleteMarkers(MARKER_INFO, false, IResource.DEPTH_ZERO);
		} catch (CoreException e) {
			IsabelleEclipsePlugin.log(e.getLocalizedMessage(), e);
		}
	}
	
	private int getDocumentLength() {
		
		if (editor.getDocument() == null) {
			return 0;
		}
		
		return editor.getDocument().getLength();
	}
	
	private void createMarkupMarkers(AnnotationConfig anns, Snapshot snapshot) throws CoreException {
		
		String[] messageMarkups = new String[] { Markup.WRITELN(), Markup.WARNING(), Markup.ERROR() };
		Iterator<Info<MarkupMessage>> messageRanges = 
			SnapshotUtil.selectMarkupMessages(snapshot, messageMarkups, new Range(0, getDocumentLength()));
		
		while (messageRanges.hasNext()) {
			Info<MarkupMessage> info = messageRanges.next();
			Range range = info.range();
			String markup = info.info().getName();
			String message = info.info().getText();
			
			if (Markup.WRITELN().equals(markup)) {
				createMarkupMessageMarker(anns, MARKER_INFO, IMarker.SEVERITY_INFO, range, message);
			} else if (Markup.WARNING().equals(markup)) {
				if (info.info().isLegacy()) {
					// TODO special legacy icon?
					createMarkupMessageMarker(anns, MARKER_PROBLEM, IMarker.SEVERITY_WARNING, range, message);
				} else {
					createMarkupMessageMarker(anns, MARKER_PROBLEM, IMarker.SEVERITY_WARNING, range, message);
				}
			} else if (Markup.ERROR().equals(markup)) {
				createMarkupMessageMarker(anns, MARKER_PROBLEM, IMarker.SEVERITY_ERROR, range, message);
			}
		}
	}
	
	private void createMarkupMessageMarker(AnnotationConfig anns, String type, 
			int severity, Range range, String message) throws CoreException {
		
		Map<String, Object> markerAttrs = new HashMap<String, Object>();
		
		markerAttrs.put(IMarker.SEVERITY, severity);
		markerAttrs.put(IMarker.CHAR_START, range.start());
		markerAttrs.put(IMarker.CHAR_END, range.stop());
		try {
			if (editor.getDocument() != null) {
				int line = editor.getDocument().getLineOfOffset(range.start()) + 1;
				markerAttrs.put(IMarker.LOCATION, "line " + line);	
			}
		} catch (BadLocationException ex) {
			// ignore
		}
		markerAttrs.put(IMarker.MESSAGE, message);
		
		anns.addMarker(type, markerAttrs);
	}
	
	
	private void updateAnnotations(AnnotationConfig anns, Session session, Snapshot snapshot) throws CoreException {
		
		IDocument document = editor.getDocument();
		if (document == null) {
			return;
		}
		
		int editorLineCount = document.getNumberOfLines();
		
		for (int line = 0; line < editorLineCount; line++) {
			
			try {
				
				int lineStart = document.getLineOffset(line);
				int lineLength = document.getLineLength(line);
				
				Range lineRange = properLineRange(document, lineStart, lineStart + lineLength);
				
				createCommandStatusAnnotations(anns, session, snapshot, lineRange);
				createMarkupAnnotations(anns, session, snapshot, lineRange);
			
			} catch (BadLocationException ex) {
				// ignore bad location
			}
		}
	}

	private IResource getMarkerResource() {
		return IsabelleFileDocumentProvider.getMarkerResource(editor.getEditorInput());
	}
	
	private AnnotationModel getAnnotationModel(IAnnotationModelExtension baseModel, Object key) {
		
		AnnotationModel model = (AnnotationModel) baseModel.getAnnotationModel(key);
		if (model == null) {
			model = new AnnotationModel();
			baseModel.addAnnotationModel(key, model);
		}
		
		return model;
	}

	private Range properLineRange(IDocument document, int start, int end) {
		int stop = start < end ? end - 1 : Math.min(end, document.getLength());
		return new Range(start, stop);
	}
	
	private void createCommandStatusAnnotations(AnnotationConfig anns,
			Session session, Snapshot snapshot, Range lineRange) {
		
		// get annotations for command status
		Iterator<Tuple2<Command, Object>> it = snapshot.node().command_range(snapshot.revert(lineRange));
		while (it.hasNext()) {
			
			Tuple2<Command, Object> commandTuple = it.next();
			Command command = commandTuple._1();
			int commandStart = (Integer) commandTuple._2();
			
			if (command.is_ignored()) {
				continue;
			}
			
			String annotationType = getStatusAnnotationType(session, snapshot, command);
			
			if (annotationType == null) {
				continue;
			}
			
			Range range = lineRange.restrict(snapshot.convert(command.range().$plus(commandStart)));
			
			addAnnotation(anns, annotationType, range);
		}
	}

	private String getStatusAnnotationType(Session session, Snapshot snapshot, Command command) {
		
		if (snapshot.is_outdated()) {
//			System.out.println("Command " + command.toString() + " is outdated");
			return ANNOTATION_OUTDATED;
		}
		
		State commandState = snapshot.command_state(command);
		Isar_Document.Status commandStatus = ISAR_DOCUMENT.command_status(commandState.status());
		
		if (commandStatus == UNPROCESSED) {
			// TODO use Isabelle's colors? At least as preference defaults?
			return ANNOTATION_UNPROCESSED;
		}
		
		if (commandStatus instanceof Forked && ((Forked) commandStatus).forks() > 0) {
			return ANNOTATION_UNFINISHED;
		}
		
		return null;
	}

	private void createMarkupAnnotations(AnnotationConfig anns,
			Session session, Snapshot snapshot, Range lineRange) {
		
		// get annotations for command status
		Iterator<Info<String>> markupRanges = SnapshotUtil.selectMarkupNames(snapshot, 
				new String[] { Markup.BAD(), Markup.HILITE(), Markup.TOKEN_RANGE() }, lineRange);
		while (markupRanges.hasNext()) {
			Info<String> info = markupRanges.next();
			Range range = info.range();
			String markup = info.info();
			
			String annotationType = getMarkupAnnotationType(markup);
			
			if (annotationType == null) {
				continue;
			}
			
			addAnnotation(anns, annotationType, range);
		}
	}
	
	private String getMarkupAnnotationType(String markup) {
		
		if (Markup.TOKEN_RANGE().equals(markup)) {
			return ANNOTATION_TOKEN;
		}
		
		if (Markup.BAD().equals(markup)) {
			return ANNOTATION_BAD;
		}
		
		return null;
	}
	
	private void addAnnotation(AnnotationConfig anns, String annotationType, Range range) {
		Annotation annotation = new Annotation(false);
		annotation.setType(annotationType);
		
		Position position = new Position(range.start(), range.stop() - range.start());
		anns.addAnnotation(annotation, position);
	}
	
	private static class AnnotationConfig {
		private final Map<Annotation, Position> annotations = 
				new HashMap<Annotation, Position>();
		
		private final List<Entry<String, Map<String, Object>>> markers = 
				new ArrayList<Entry<String, Map<String, Object>>>();
		
		public void addAnnotation(Annotation ann, Position pos) {
			annotations.put(ann, pos);
		}
		
		public void addMarker(String type, Map<String, Object> attrs) {
			markers.add(new SimpleEntry<String, Map<String, Object>>(type, attrs));
		}
	}
	
	private class AnnotationUpdateJob extends Job {

		private final Set<Command> commands;
		
		public AnnotationUpdateJob(Set<Command> commands) {
			super("Updating theory annotations");
			this.commands = new HashSet<Command>(commands);
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			
			AnnotationConfig anns = createMarkers(commands);
			
			// check if cancelled - then do not set the outline
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			
			setMarkers(anns);
			
			return Status.OK_STATUS;
		}
	}
}
