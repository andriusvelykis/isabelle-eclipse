package isabelle.eclipse.editors;

import isabelle.Command;
import isabelle.Document.Node;
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
import isabelle.eclipse.core.util.SafeSessionActor;
import isabelle.eclipse.util.SessionEventSupport;
import isabelle.scala.ISessionCommandsListener;
import isabelle.scala.SessionActor;
import isabelle.scala.SessionEventType;
import isabelle.scala.SnapshotUtil;
import isabelle.scala.SnapshotUtil.MarkupMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.ui.texteditor.MarkerUtilities;

import scala.Option;
import scala.Tuple2;

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
	
	/**
	 * A rule to ensure that annotation update jobs are synchronized and not
	 * overlapping
	 */
	private final ISchedulingRule syncUpdateJobRule = new ISchedulingRule() {
		
		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return rule == this;
		}
		
		@Override
		public boolean contains(ISchedulingRule rule) {
			return rule == this;
		}
	};
	
	private final SessionEventSupport sessionEvents;
	
	public TheoryAnnotations(TheoryEditor editor) {
		super();
		this.editor = editor;
		
		// When commands change (e.g. results from the prover), update the
		// annotations accordingly.
		sessionEvents = new SessionEventSupport(EnumSet.of(SessionEventType.COMMAND)) {
			
			@Override
			protected SessionActor createSessionActor(Session session) {
				return new SafeSessionActor().commandsChanged(new ISessionCommandsListener() {
					
					@Override
					public void commandsChanged(Commands_Changed changed) {
						
						DocumentModel isabelleModel = TheoryAnnotations.this.editor.getIsabelleModel();
						if (isabelleModel == null) {
							// no model available, so cannot get a snapshot and annotations
							return;
						}
						
						// avoid updating annotations if commands are from a different document
						if (changed.nodes().contains(isabelleModel.getName().getRef())) {
							updateAnnotations(setAsJavaSet(changed.commands()));
						}
					}
				});
			}

			@Override
			protected void sessionInit(Session session) {
				// when the session is initialised, update all annotations from scratch
				updateAllAnnotations();
			}
		};
	}
	
	public void dispose() {
		sessionEvents.dispose();
	}
	
	public void updateAllAnnotations() {
		
		DocumentModel isabelleModel = editor.getIsabelleModel();
		if (isabelleModel == null) {
			return;
		}
		
		Set<Command> snapshotCommands = setAsJavaSet(isabelleModel.getSnapshot().node().commands());
		updateAnnotations(snapshotCommands);
	}
	
	private void updateAnnotations(Set<Command> commands) {
		
		Job updateJob = new AnnotationUpdateJob(commands);
		// synchronise the jobs - only a single update can be running
		updateJob.setRule(syncUpdateJobRule);
		// top priority to give quick feedback to the user
		updateJob.setPriority(Job.INTERACTIVE);
//		updateJob.setPriority(Job.DECORATE);
		// system job - do not display progress in the UI
		updateJob.setSystem(true);
		
		updateJob.schedule();
	}
	
	private AnnotationConfig createAnnotations(Set<Command> commands) {
		
		DocumentModel isabelleModel = editor.getIsabelleModel();
		if (isabelleModel == null) {
			// no model available, so cannot create new markers
			// do not delete old persistent markers if present
			return null;
		}
		
		Snapshot snapshot = isabelleModel.getSnapshot();
		
		// copy commands set
		commands = new LinkedHashSet<Command>(commands);
		// only use commands that are in the snapshot
		commands.retainAll(setAsJavaSet(snapshot.node().commands()));

		if (commands.isEmpty()) {
			// no/outdated? commands
			return null;
		}
		
		/*
		 * Mark snapshot version - it is necessary since actual setting of
		 * annotations is done asynchronously, and the annotation positions may
		 * no longer apply (causes BadLocationException when setting).
		 */
		long snapshotVersion = snapshot.version().id();
		
		// get the ranges occupied by the changed commands
		// and recalculate annotations for them afterwards
		List<Range> mergeRanges = getChangedRanges(snapshot, commands);
		
		AnnotationConfig anns = new AnnotationConfig(editor, snapshotVersion, mergeRanges);
		
		// restrict the ranges to document length
		Range maxRange = isabelleModel.getDocumentRange();
		
		// create annotations (status/markup) and markers (errors/warnings)
		createAnnotations(anns, isabelleModel.getSession(), snapshot, maxRange, mergeRanges);
		createMarkers(anns, snapshot, maxRange, mergeRanges);
		
		return anns;
	}

	/**
	 * Calculate document ranges for the changed commands.
	 * 
	 * @param snapshot
	 * @param commands
	 * @return
	 */
	private List<Range> getChangedRanges(Snapshot snapshot, Set<Command> commands) {
		
		List<Range> commandRanges = new ArrayList<Range>(); 
		
		// take all commands and get the ranges for the given ones
		for (CommandRangeIterator it = new CommandRangeIterator(
				snapshot.node().command_range(0)); it.hasNext(); ) {
			
			Tuple2<Command, Range> commandTuple = it.next();
			Command command = commandTuple._1();
			
			if (!commands.contains(command)) {
				// not in the set
				continue;
			}
			
			Range cmdRange = commandTuple._2();
			commandRanges.add(cmdRange);
		}
		
		// merge overlapping/adjoining ranges
		return mergeRanges(commandRanges);
	}
	
	/**
	 * Merges overlapping/adjoined ranges
	 * 
	 * @param ranges
	 * @return
	 */
	private static List<Range> mergeRanges(Collection<Range> ranges) {
		
		List<Range> sorted = new ArrayList<Range>(ranges);
		Collections.sort(sorted, new Comparator<Range>() {
			@Override
			public int compare(Range o1, Range o2) {
				return o1.compare(o2);
			}
		});
		
		List<Range> mergedRanges = new ArrayList<Range>();
		
		Range prev = null;
		for (Range range : ranges) {
			
			if (prev == null) {
				// first encountered - just mark the range
				prev = range;
			} else {
				// either the ranges overlap, or the gap between them is too small
				if (range.start() - prev.stop() <= 1) {
					// merge
					Range newRange = new Range(
							Math.min(prev.start(), range.start()), 
							Math.max(prev.stop(), range.stop()));
					prev = newRange;
				} else {
					// not overlapping ranges - store the last one and mark the new
					mergedRanges.add(prev);
					prev = range;
				}
			}
		}
		
		if (prev != null) {
			mergedRanges.add(prev);
		}
		
		return mergedRanges;
	}
	
	/**
	 * Creates the markers (error/warning). Only marker information is created
	 * and stored in {@link AnnotationConfig}, which later instantiates them.
	 * 
	 * @param anns
	 * @param snapshot
	 * @param maxRange
	 * @param ranges
	 */
	private void createMarkers(AnnotationConfig anns, Snapshot snapshot,
			Range maxRange, List<Range> ranges) {
		
		String[] messageMarkups = new String[] { Markup.WRITELN(), Markup.WARNING(), Markup.ERROR() };
		
		for (Range range : ranges) {
			
			// get markup messages in every range and populate marker information
			scala.collection.Iterator<Info<MarkupMessage>> messageRanges = 
				SnapshotUtil.selectMarkupMessages(snapshot, messageMarkups, range);
			
			while (messageRanges.hasNext()) {
				Info<MarkupMessage> info = messageRanges.next();
				Range markupRange = info.range();
				String markup = info.info().getName();
				String message = info.info().getText();
				
				if (Markup.WRITELN().equals(markup)) {
					createMarkupMessageMarker(anns, MARKER_INFO, IMarker.SEVERITY_INFO, markupRange, message, maxRange);
				} else if (Markup.WARNING().equals(markup)) {
					if (info.info().isLegacy()) {
						// TODO special legacy icon?
						createMarkupMessageMarker(anns, MARKER_PROBLEM, IMarker.SEVERITY_WARNING, markupRange, message, maxRange);
					} else {
						createMarkupMessageMarker(anns, MARKER_PROBLEM, IMarker.SEVERITY_WARNING, markupRange, message, maxRange);
					}
				} else if (Markup.ERROR().equals(markup)) {
					createMarkupMessageMarker(anns, MARKER_PROBLEM, IMarker.SEVERITY_ERROR, markupRange, message, maxRange);
				}
			}
		
		}
	}
	
	private void createMarkupMessageMarker(AnnotationConfig anns, String type, 
			int severity, Range range, String message, Range maxRange) {
		
		// restrict the range to avoid exceeding the document range
		Option<Range> fixedRangeOpt = maxRange.try_restrict(range);
		Range fixedRange;
		if (fixedRangeOpt.isEmpty()) {
			// invalid range (e.g. outside the max range)
			// do not ignore, but better display it at (0, 0)
			fixedRange = new Range(0, 0);
		} else {
			fixedRange = fixedRangeOpt.get();
		}
		
		Map<String, Object> markerAttrs = new HashMap<String, Object>();
		
		markerAttrs.put(IMarker.SEVERITY, severity);
		MarkerUtilities.setCharStart(markerAttrs, fixedRange.start());
		MarkerUtilities.setCharEnd(markerAttrs, fixedRange.stop());
		try {
			if (editor.getDocument() != null) {
				int line = editor.getDocument().getLineOfOffset(fixedRange.start()) + 1;
				markerAttrs.put(IMarker.LOCATION, "line " + line);
				MarkerUtilities.setLineNumber(markerAttrs, line);
			}
		} catch (BadLocationException ex) {
			// ignore
		}
		MarkerUtilities.setMessage(markerAttrs, message);
		
		anns.addMarker(type, markerAttrs);
	}
	
	private void createAnnotations(AnnotationConfig anns, Session session, Snapshot snapshot,
			Range maxRange, List<Range> ranges) {
		
		for (Range range : ranges) {
			createStatusAnnotations(anns, session, snapshot, maxRange, range);
			createMarkupAnnotations(anns, session, snapshot, maxRange, range);
		}
	}
	
	/*
	 * Creates command status annotations (e.g. unprocessed/outdated, etc.)
	 */
	private void createStatusAnnotations(AnnotationConfig anns,
			Session session, Snapshot snapshot, Range maxRange, Range range) {
		
		// get annotations for command status in the given range
		for (CommandRangeIterator it = new CommandRangeIterator(
				snapshot.node().command_range(range)); it.hasNext(); ) {
			
			Tuple2<Command, Range> commandTuple = it.next();
			Command command = commandTuple._1();
			Range commandRange = commandTuple._2();
			
			if (command.is_ignored()) {
				continue;
			}
			
			String annotationType = getStatusAnnotationType(session, snapshot, command);
			
			if (annotationType == null) {
				continue;
			}
			
			Range actualRange = snapshot.convert(commandRange);
			addAnnotation(anns, annotationType, actualRange, maxRange);
		}
	}

	private String getStatusAnnotationType(Session session, Snapshot snapshot, Command command) {
		
		if (snapshot.is_outdated()) {
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
			Session session, Snapshot snapshot, Range maxRange, Range range) {
		
		// get annotations for command status
		scala.collection.Iterator<Info<String>> markupRanges = SnapshotUtil.selectMarkupNames(snapshot, 
				new String[] { Markup.BAD(), Markup.HILITE(), Markup.TOKEN_RANGE() }, range);
		while (markupRanges.hasNext()) {
			Info<String> info = markupRanges.next();
			Range markupRange = info.range();
			String markup = info.info();
			
			String annotationType = getMarkupAnnotationType(markup);
			
			if (annotationType == null) {
				continue;
			}
			
			addAnnotation(anns, annotationType, markupRange, maxRange);
		}
	}
	
	private String getMarkupAnnotationType(String markup) {
		
		if (Markup.TOKEN_RANGE().equals(markup)) {
			return ANNOTATION_TOKEN;
		}
		
		if (Markup.BAD().equals(markup)) {
			return ANNOTATION_BAD;
		}
		
		// TODO HILITE
		
		return null;
	}
	
	private void addAnnotation(AnnotationConfig anns, String annotationType, Range range, Range maxRange) {
		Annotation annotation = new Annotation(false);
		annotation.setType(annotationType);
		
		// try to restrict the range to document limits
		Option<Range> fixedRangeOpt = maxRange.try_restrict(range);
		if (fixedRangeOpt.isEmpty()) {
			// invalid range (e.g. outside the max range)
			// so ignore the annotation altogether
			return;
		}
		
		Range fixedRange = fixedRangeOpt.get();
		Position position = new Position(fixedRange.start(), getLength(fixedRange));
		
		anns.addAnnotation(annotation, position);
	}
	
	static int getLength(Range range) {
		return range.stop() - range.start();
	}
	
	/**
	 * A private job to update annotations in a separate thread. First
	 * calculates the annotations, then sets them in the UI thread.
	 * 
	 * @author Andrius Velykis
	 */
	private class AnnotationUpdateJob extends Job {

		private final Set<Command> commands;
		
		public AnnotationUpdateJob(Set<Command> commands) {
			super("Updating theory annotations");
			this.commands = new HashSet<Command>(commands);
		}
		
		@Override
		public IStatus run(IProgressMonitor monitor) {
			
			final AnnotationConfig anns = createAnnotations(commands);
			if (anns != null) {
				/*
				 * Set the annotations/markers in UI thread, otherwise getting
				 * ConcurrentModificationException on the document positions
				 * (e.g. when setting the annotations and repainting them at the
				 * same time)
				 */
				editor.getSite().getShell().getDisplay().asyncExec(new Runnable() {
					
					@Override
					public void run() {
						anns.setAnnotations();
					}
				});
			}
			
			return Status.OK_STATUS;
		}
	}
	
	/**
	 * A convenience iterator to calculate command and its full range. The
	 * {@link Node} command iterator gives command and its start only.
	 * 
	 * @author Andrius Velykis
	 */
	private static class CommandRangeIterator implements Iterator<Tuple2<Command, Range>> {

		private final scala.collection.Iterator<? extends Tuple2<Command, ?>> commandStartIterator;
		
		public CommandRangeIterator(
				scala.collection.Iterator<? extends Tuple2<Command, ?>> commandStartIterator) {
			super();
			this.commandStartIterator = commandStartIterator;
		}

		@Override
		public boolean hasNext() {
			return commandStartIterator.hasNext();
		}

		@Override
		public Tuple2<Command, Range> next() {
			Tuple2<Command, ?> commandTuple = commandStartIterator.next();
			Command command = commandTuple._1();
			int commandStart = (Integer) commandTuple._2();
			Range cmdRange = command.range().$plus(commandStart);
			return new Tuple2<Command, Range>(command, cmdRange);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
