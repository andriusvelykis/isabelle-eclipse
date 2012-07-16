package isabelle.eclipse.ui.editors;

import isabelle.Command;
import isabelle.Event_Bus;
import isabelle.Document.Snapshot;
import isabelle.Session;
import isabelle.Session.Commands_Changed;
import isabelle.Text.Range;
import isabelle.eclipse.core.text.AnnotationFactory;
import isabelle.eclipse.core.text.AnnotationInfo;
import isabelle.eclipse.core.text.DocumentModel;
import isabelle.eclipse.core.util.SafeSessionActor;
import isabelle.eclipse.core.util.SessionEventSupport;
import isabelle.scala.ISessionCommandsListener;
import isabelle.scala.ScalaCollections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;

import scala.Option;
import scala.actors.Actor;
import scala.collection.immutable.Set;

import static scala.collection.JavaConversions.seqAsJavaList;
import static scala.collection.JavaConversions.asScalaBuffer;

public class TheoryAnnotations {
	
	private final TheoryEditor editor;

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
	
	private final SessionEventSupport<?> sessionEvents;
	private int lastCommandCount = 0;
	private boolean lastSnapshotOutdated = true;
	
	public TheoryAnnotations(TheoryEditor editor) {
		super();
		this.editor = editor;
		
		// When commands change (e.g. results from the prover), update the
		// annotations accordingly.
		sessionEvents = new SessionEventSupport<Commands_Changed>() {

			@Override
			public void sessionInit(Session session) {
				// when the session is initialised, update all annotations from scratch
				updateAllAnnotations();
			}
			
			@Override
			public Actor sessionActor() {
				return (Actor) new SafeSessionActor().commandsChanged(new ISessionCommandsListener() {
					
					@Override
					public void commandsChanged(Commands_Changed changed) {
						
						Option<DocumentModel> isabelleModelOpt = TheoryAnnotations.this.editor.isabelleModel();
						if (isabelleModelOpt.isEmpty()) {
							// no model available, so cannot get a snapshot and annotations
							return;
						}
						DocumentModel isabelleModel = isabelleModelOpt.get();
						
						// avoid updating annotations if commands are from a different document
						if (changed.nodes().contains(isabelleModel.name())) {
							updateAnnotations(changed.commands());
						}
					}
				}).getActor();
			}
			
			@Override
			public scala.collection.immutable.List<Event_Bus<Commands_Changed>> sessionEvents0(Session session) {
				return ScalaCollections.singletonList(session.commands_changed());
			}
		};
		sessionEvents.init();
	}
	
	public void dispose() {
		sessionEvents.dispose();
	}
	
	public void updateAllAnnotations() {
		
		Option<DocumentModel> isabelleModelOpt = editor.isabelleModel();
		if (isabelleModelOpt.isEmpty()) {
			return;
		}
		DocumentModel isabelleModel = isabelleModelOpt.get();
		
		Set<Command> snapshotCommands = isabelleModel.snapshot().node().commands();
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
		
		Option<DocumentModel> isabelleModelOpt = TheoryAnnotations.this.editor.isabelleModel();
		if (isabelleModelOpt.isEmpty()) {
			// no model available, so cannot create new markers
			// do not delete old persistent markers if present
			return null;
		}
		DocumentModel isabelleModel = isabelleModelOpt.get();
		
		Snapshot snapshot = isabelleModel.snapshot();
		
		Set<Command> snapshotCmds = snapshot.node().commands();
		if (snapshotCmds.size() > lastCommandCount || lastSnapshotOutdated) {
			/*
			 * More commands in the snapshot than was previously - update
			 * annotations for the whole snapshot. This is necessary because
			 * parsing can happen slowly and commands appear delayed in the
			 * snapshot.
			 * 
			 * This is a workaround because parsing events are not firing
			 * notifications, so we manually check if we need updating. We
			 * update if the last snapshot was outdated, or new commands were
			 * added (e.g. via parsing).
			 */
			commands = snapshotCmds;
			lastCommandCount = snapshotCmds.size();
		} else {
			// Only use commands that are in the snapshot.
			commands = commands.intersect(snapshotCmds);
		}
		
		lastSnapshotOutdated = snapshot.is_outdated();

		if (commands.isEmpty()) {
			// no/outdated? commands
			return null;
		}
		
		// get the ranges occupied by the changed commands
		// and recalculate annotations for them afterwards
		List<Range> commandRanges = seqAsJavaList(AnnotationFactory.commandRanges(snapshot, commands));
		// merge overlapping/adjoining ranges
		List<Range> mergeRanges = mergeRanges(commandRanges);
		
		List<AnnotationInfo> annotations = seqAsJavaList(AnnotationFactory.createAnnotations(snapshot, asScalaBuffer(mergeRanges).toList()));
		AnnotationConfig config = new AnnotationConfig(annotations, mergeRanges);
		
		return config;
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
	
	private void setAnnotations(AnnotationConfig config) {
		
		// replace annotations
		IAnnotationModel baseAnnotationModel = 
				editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
		if (baseAnnotationModel == null) {
			return;
		}

		// use modern models
		Assert.isTrue(baseAnnotationModel instanceof IAnnotationModelExtension);
		
		IDocument document = editor.document();
		if (document == null) {
			return;
		}
		
		IResource markerResource = EditorUtil.getResource(editor.getEditorInput());
		
		AnnotationUpdater updater = new AnnotationUpdater(
				(IAnnotationModelExtension) baseAnnotationModel, document, markerResource);
		
		updater.updateAnnotations(config.changedRanges, config.annotations);
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
			this.commands = commands;
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
				editor.getSite().getWorkbenchWindow().getWorkbench()
						.getDisplay().asyncExec(new Runnable() {

							@Override
							public void run() {
								setAnnotations(anns);
							}
						});
			}
			
			return Status.OK_STATUS;
		}
	}
	
	private static class AnnotationConfig {
		
		private final List<AnnotationInfo> annotations;
		private final List<Range> changedRanges;
		
		public AnnotationConfig(List<AnnotationInfo> annotations, List<Range> changedRanges) {
			super();
			this.annotations = new ArrayList<AnnotationInfo>(annotations);
			this.changedRanges = new ArrayList<Range>(changedRanges);
		}
	}
}
