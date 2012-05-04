package isabelle.eclipse.editors;

import isabelle.Command;
import isabelle.Isar_Document;
import isabelle.Isar_Document$;
import isabelle.Isar_Document$Unprocessed$;
import isabelle.Markup;
import isabelle.Command.State;
import isabelle.Document.Node;
import isabelle.Document.Snapshot;
import isabelle.Isar_Document.Forked;
import isabelle.Text.Info;
import isabelle.Text.Range;
import isabelle.scala.SnapshotUtil;
import isabelle.scala.SnapshotUtil.MarkupMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import scala.Tuple2;
import scala.collection.Iterator;


/**
 * A static factory to create various Isabelle annotations for a document snapshot.
 * <p>
 * The annotations are created in a generic way using {@link AnnotationInfo} data, so that users can
 * later implement them as they wish (e.g. via document annotations or markers, where applicable).
 * </p>
 * <p>
 * This class encapsulates the calculation of the annotations, which types are given in
 * {@link IsabelleAnnotation}.
 * </p>
 * 
 * @author Andrius Velykis
 */
public class AnnotationFactory {
	
	// Isabelle/Scala Object references (to have nicer references in the code)
	private static final Isar_Document$ ISAR_DOCUMENT = Isar_Document$.MODULE$;
	private static final Isar_Document$Unprocessed$ UNPROCESSED = Isar_Document$Unprocessed$.MODULE$;
	
	/**
	 * Markups in the snapshot that have associated messages and should be created as annotations.
	 */
	private static final String[] MESSAGE_MARKUPS = new String[] { 
			Markup.WRITELN(), Markup.WARNING(), Markup.ERROR() };
	
	/**
	 * Markups in the snapshot that should be displayed in the document as decorations.
	 */
	private static final String[] HIGHLIGHT_MARKUPS = new String[] { 
		Markup.BAD(), Markup.HILITE(), Markup.TOKEN_RANGE() };

	/**
	 * Type of calculated annotation.
	 * 
	 * @author Andrius Velykis
	 */
	public enum IsabelleAnnotation {
		MARKUP_BAD,
		MARKUP_HILITE,
		MARKUP_TOKEN_RANGE,
		MESSAGE_ERROR,
		MESSAGE_LEGACY,
		MESSAGE_WARNING,
		MESSAGE_WRITELN,
		STATUS_OUTDATED,
		STATUS_UNFINISHED,
		STATUS_UNPROCESSED
	}
	
	/**
	 * Calculates abstract annotations for the given ranges in the snapshot. 
	 * 
	 * @param snapshot
	 * @param ranges
	 * @return
	 */
	public static List<AnnotationInfo> createAnnotations(Snapshot snapshot, List<Range> ranges) {
		
		// use set to avoid duplicates
		Set<AnnotationInfo> annotations = new HashSet<AnnotationInfo>();
		
		// for each range calculate the different annotations
		for (Range range : ranges) {
			annotations.addAll(createStatusAnnotations(snapshot, range));
			annotations.addAll(createMarkupAnnotations(snapshot, range));
			annotations.addAll(createMessageAnnotations(snapshot, range));
		}
		
		List<AnnotationInfo> sortedAnns = new ArrayList<AnnotationInfo>(annotations);
		
		/*
		 * Sort the annotations by the end offset. This is useful in case the
		 * document changes (becomes shorter) before the annotations are set. In
		 * this case, the annotations which are too long/too late give a
		 * BadLocationException. If the annotations are not ordered, this may
		 * cause some of the annotations to go missing. If sorted, only the last
		 * ones (outside the document) get lost, which is acceptable.
		 */
		Collections.sort(sortedAnns, new Comparator<AnnotationInfo>() {
			@Override
			public int compare(AnnotationInfo o1, AnnotationInfo o2) {
				int end1 = o1.getRange().stop();
				int end2 = o2.getRange().stop();
				return end1 - end2;
			}
		});
		
		return sortedAnns;
	}
	
	/**
	 * Creates command status annotations (e.g. unprocessed/outdated, etc.)
	 * 
	 * @param snapshot
	 * @param range
	 * @return
	 */
	private static List<AnnotationInfo> createStatusAnnotations(Snapshot snapshot, Range range) {
		
		List<AnnotationInfo> annotations = new ArrayList<AnnotationInfo>();
		
		// get annotations for command status in the given range
		for (CommandRangeIterator it = new CommandRangeIterator(
				snapshot.node().command_range(range)); it.hasNext(); ) {
			
			Tuple2<Command, Range> commandTuple = it.next();
			Command command = commandTuple._1();
			Range commandRange = commandTuple._2();
			
			if (command.is_ignored()) {
				continue;
			}
			
			IsabelleAnnotation annotationType = getStatusAnnotationType(snapshot, command);
			
			if (annotationType == null) {
				continue;
			}
			
			Range actualRange = snapshot.convert(commandRange);
			
			annotations.add(new AnnotationInfo(annotationType, actualRange));
		}
		
		return annotations;
	}

	private static IsabelleAnnotation getStatusAnnotationType(Snapshot snapshot, Command command) {
		
		if (snapshot.is_outdated()) {
			return IsabelleAnnotation.STATUS_OUTDATED;
		}
		
		State commandState = snapshot.command_state(command);
		Isar_Document.Status commandStatus = ISAR_DOCUMENT.command_status(commandState.status());
		
		if (commandStatus == UNPROCESSED) {
			return IsabelleAnnotation.STATUS_UNPROCESSED;
		}
		
		if (commandStatus instanceof Forked && ((Forked) commandStatus).forks() > 0) {
			return IsabelleAnnotation.STATUS_UNFINISHED;
		}
		
		return null;
	}

	/**
	 * Creates markup annotations (e.g. ranges which should be highlighted in some way)
	 * 
	 * @param snapshot
	 * @param range
	 * @return
	 */
	private static List<AnnotationInfo> createMarkupAnnotations(Snapshot snapshot, Range range) {
		
		List<AnnotationInfo> annotations = new ArrayList<AnnotationInfo>();
		
		// get annotations for command status
		Iterator<Info<String>> markupRanges = SnapshotUtil.selectMarkupNames(
				snapshot, HIGHLIGHT_MARKUPS, range);
		
		while (markupRanges.hasNext()) {
			Info<String> info = markupRanges.next();
			Range markupRange = info.range();
			String markup = info.info();
			
			IsabelleAnnotation annotationType = getMarkupAnnotationType(markup);
			
			if (annotationType == null) {
				continue;
			}
			
			annotations.add(new AnnotationInfo(annotationType, markupRange));
		}
		
		return annotations;
	}
	
	private static IsabelleAnnotation getMarkupAnnotationType(String markup) {
		
		if (Markup.TOKEN_RANGE().equals(markup)) {
			return IsabelleAnnotation.MARKUP_TOKEN_RANGE;
		}
		
		if (Markup.BAD().equals(markup)) {
			return IsabelleAnnotation.MARKUP_BAD;
		}
		
		if (Markup.HILITE().equals(markup)) {
			return IsabelleAnnotation.MARKUP_HILITE;
		}
		
		return null;
	}
	
	/**
	 * Creates message annotations (e.g. errors/warnings)
	 * 
	 * @param snapshot
	 * @param range
	 * @return
	 */
	private static List<AnnotationInfo> createMessageAnnotations(Snapshot snapshot, Range range) {
		
		List<AnnotationInfo> annotations = new ArrayList<AnnotationInfo>();
		
		// get markup messages in every range and populate marker information
		Iterator<Info<MarkupMessage>> messageRanges = 
			SnapshotUtil.selectMarkupMessages(snapshot, MESSAGE_MARKUPS, range);
		
		while (messageRanges.hasNext()) {
			Info<MarkupMessage> info = messageRanges.next();
			Range markupRange = info.range();
			String message = info.info().getText();
			
			IsabelleAnnotation annotationType = getMessageAnnotationType(info.info());
			
			if (annotationType == null) {
				continue;
			}
			
			annotations.add(new AnnotationInfo(annotationType, markupRange, message));
		}
		
		return annotations;
	}
	
	private static IsabelleAnnotation getMessageAnnotationType(MarkupMessage info) {
		
		String markup = info.getName();
		
		if (Markup.WRITELN().equals(markup)) {
			return IsabelleAnnotation.MESSAGE_WRITELN;
		}
		
		if (Markup.WARNING().equals(markup)) {
			if (info.isLegacy()) {
				return IsabelleAnnotation.MESSAGE_LEGACY;
			} else {
				return IsabelleAnnotation.MESSAGE_WARNING;
			}
		}
		
		if (Markup.ERROR().equals(markup)) {
			return IsabelleAnnotation.MESSAGE_ERROR;
		}
		
		return null;
	}
	
	/**
	 * Calculates document ranges for the given commands.
	 * 
	 * @param snapshot
	 * @param commands
	 * @return
	 */
	public static List<Range> getCommandRanges(Snapshot snapshot, Set<Command> commands) {
		
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
		
		return commandRanges;
	}
	
	/**
	 * Abstract representation of an annotation, carrying its type, position (range) and message.
	 * 
	 * @author Andrius Velykis
	 */
	public static class AnnotationInfo {
		
		private final IsabelleAnnotation type;
		private final Range range;
		private final String message;
		
		public AnnotationInfo(IsabelleAnnotation type, Range range) {
			this(type, range, null);
		}
		
		public AnnotationInfo(IsabelleAnnotation type, Range range, String message) {
			this.type = type;
			this.range = range;
			this.message = message;
		}

		public IsabelleAnnotation getType() {
			return type;
		}

		public Range getRange() {
			return range;
		}

		public String getMessage() {
			return message;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((message == null) ? 0 : message.hashCode());
			result = prime * result + ((range == null) ? 0 : range.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AnnotationInfo other = (AnnotationInfo) obj;
			if (message == null) {
				if (other.message != null)
					return false;
			} else if (!message.equals(other.message))
				return false;
			if (range == null) {
				if (other.range != null)
					return false;
			} else if (!range.equals(other.range))
				return false;
			if (type != other.type)
				return false;
			return true;
		}
	}
	
	/**
	 * A convenience iterator to calculate command and its full range. The
	 * {@link Node} command iterator gives command and its start only.
	 * 
	 * @author Andrius Velykis
	 */
	private static class CommandRangeIterator implements java.util.Iterator<Tuple2<Command, Range>> {

		private final Iterator<? extends Tuple2<Command, ?>> commandStartIterator;
		
		public CommandRangeIterator(Iterator<? extends Tuple2<Command, ?>> commandStartIterator) {
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
