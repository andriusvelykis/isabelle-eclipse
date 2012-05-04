package isabelle.eclipse.editors;

import isabelle.Text.Range;
import isabelle.eclipse.IsabelleEclipsePlugin;
import isabelle.eclipse.core.text.DocumentModel;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;

/**
 * A class to store annotation configuration - created annotations and markers.
 * It also encapsulates methods to set the annotations on the editor/resource.
 * 
 * @author Andrius Velykis
 */
class AnnotationConfig {

	/**
	 * A child annotation model is used referenced with this key.
	 */
	private static final Object ANNOTATION_MODEL_KEY = new Object();
	
	// note the map removes duplicate annotations (same pos/type)
	private final Map<TypePos, Annotation> annotations = 
			new HashMap<TypePos, Annotation>();
	
	private final Set<Entry<String, Map<String, Object>>> markers = 
			new LinkedHashSet<Entry<String, Map<String, Object>>>();
	
	private final List<Range> changedRanges;
	
	private final TheoryEditor editor;
	
	public AnnotationConfig(TheoryEditor editor, List<Range> changedRanges) {
		this.editor = editor;
		this.changedRanges = new ArrayList<Range>(changedRanges);
	}
	
	public void addAnnotation(Annotation ann, Position pos) {
		annotations.put(new TypePos(ann.getType(), pos), ann);
	}
	
	public void addMarker(String type, Map<String, Object> attrs) {
		markers.add(new SimpleEntry<String, Map<String, Object>>(type, attrs));
	}
	
	/**
	 * Sets the stored annotations onto the editor/resource. Does not recreate
	 * annotations/markers if the same ones already exist.
	 */
	public void setAnnotations() {
		
		DocumentModel isabelleModel = editor.getIsabelleModel();
		if (isabelleModel == null) {
			return;
		}
		
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
				ANNOTATION_MODEL_KEY);
		
		List<Annotation> changedAnns = getChangedAnnotations(annotationModel, changedRanges);
		List<Annotation> deleteAnns = filterExistingAnnotations(annotationModel, changedAnns);

		Annotation[] deleteAnnsArray = deleteAnns.toArray(new Annotation[deleteAnns.size()]);
		Map<Annotation, Position> addAnns = getAnnotationMap();
		
		IResource markerResource = EditorUtil.getResource(editor.getEditorInput());
		if (markerResource != null) {
			// resource is not available to add markers (e.g. the file is not in the workspace)
			// TODO show annotations instead?
			List<IMarker> changedMarkers = getChangedMarkers(markerResource, changedRanges);
			List<IMarker> deleteMarkers = filterExistingMarkers(changedMarkers);
			Collection<Entry<String, Map<String, Object>>> addMarkers = markers;
			setMarkers(markerResource, deleteMarkers, addMarkers);
		}
		
		annotationModel.replaceAnnotations(deleteAnnsArray, addAnns);
	}
	
	private AnnotationModel getAnnotationModel(IAnnotationModelExtension baseModel, Object key) {
		
		AnnotationModel model = (AnnotationModel) baseModel.getAnnotationModel(key);
		if (model == null) {
			model = new AnnotationModel();
			if (baseModel instanceof ISynchronizable) {
				model.setLockObject(((ISynchronizable) baseModel).getLockObject());
			}
			baseModel.addAnnotationModel(key, model);
		}
		
		return model;
	}

	private void setMarkers(final IResource markerResource,
			final List<IMarker> deleteMarkers,
			final Collection<Entry<String, Map<String, Object>>> addMarkers) {
		
		// add new markers
		IWorkspaceRunnable r = new IWorkspaceRunnable() {
			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				
				for (IMarker marker : deleteMarkers) {
					marker.delete();
				}
				
				for (Entry<String, Map<String, Object>> markerInfo : addMarkers) {
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
	
	private List<Annotation> getChangedAnnotations(AnnotationModel annotationModel, 
			List<Range> changeRange) {
		
		if (changeRange.isEmpty()) {
			// create a single "full range"
			changeRange = Arrays.asList(new Range(0, Integer.MAX_VALUE - 1));
		}

		Set<Annotation> changedAnns = new LinkedHashSet<Annotation>();
		
		// find annotations in every range and consider them changed for examination
		for (Range range : changeRange) {
			for (Iterator<?> it = annotationModel.getAnnotationIterator(
					range.start(), TheoryAnnotations.getLength(range), true, true); 
					it.hasNext(); ) {
				
				changedAnns.add((Annotation) it.next());
			}
		}
		
		return new ArrayList<Annotation>(changedAnns);
	}
	
	private List<Annotation> filterExistingAnnotations(IAnnotationModel annotationModel, 
			List<Annotation> existingAnns) {
		
		List<Annotation> deleteAnns = new ArrayList<Annotation>();
		
		// check if annotation with the matching type/position is in the new state
		for (Annotation ann : existingAnns) {
			Position annPos = annotationModel.getPosition(ann);
			
			// remove matching annotations from the new state
			Annotation newAnn = annotations.remove(new TypePos(ann.getType(), annPos));
			
			if (newAnn == null) {
				// no corresponding annotation in the new state, so delete the existing one
				deleteAnns.add(ann);
			}
		}
		
		return deleteAnns;
	}
	
	private List<IMarker> getChangedMarkers(IResource markerResource, List<Range> changeRange) {
		
		List<IMarker> allMarkers = new ArrayList<IMarker>();
		try {
			allMarkers.addAll(Arrays.asList(
					markerResource.findMarkers(TheoryAnnotations.MARKER_PROBLEM, false, IResource.DEPTH_ZERO)));
			allMarkers.addAll(Arrays.asList(
					markerResource.findMarkers(TheoryAnnotations.MARKER_INFO, false, IResource.DEPTH_ZERO)));
		} catch (CoreException e) {
			IsabelleEclipsePlugin.log(e.getLocalizedMessage(), e);
		}
		
		if (changeRange.isEmpty()) {
			// use all markers
			return allMarkers;
		}

		List<IMarker> changedMarkers = new ArrayList<IMarker>();

		// find markers that overlap with the given ranges, they are considered
		// changed and will be examined
		for (IMarker marker : allMarkers) {
			Range markerRange = new Range(
					marker.getAttribute(IMarker.CHAR_START, 0),
					marker.getAttribute(IMarker.CHAR_END, Integer.MAX_VALUE));
			
			for (Range range : changeRange) {
				if (markerRange.overlaps(range)) {
					changedMarkers.add(marker);
					break;
				}
			}
		}
		
		return changedMarkers;
	}
	
	private List<IMarker> filterExistingMarkers(List<IMarker> markers) {
		
		List<IMarker> deleteMarkers = new ArrayList<IMarker>();
		
		for (IMarker marker : markers) {
			
			try {
				// create a marker information entry to check within the new markers
				Entry<String, Map<String, Object>> markerInfo = new SimpleEntry<String, Map<String, Object>>(
						marker.getType(), marker.getAttributes());
				
				// remove matching markers from the new state
				boolean newMarker = markers.remove(markerInfo);
				if (newMarker) {
					// found a corresponding marker in the new state, so keep the existing one
					continue;
				}
				
			} catch (CoreException e) {
				IsabelleEclipsePlugin.log(e.getLocalizedMessage(), e);
			}
			
			// did not match with a corresponding marker in the new state, so delete it
			deleteMarkers.add(marker);
		}
		
		return deleteMarkers;
	}
	
	private Map<Annotation, Position> getAnnotationMap() {
		
		List<Entry<TypePos, Annotation>> sortedAnns = new ArrayList<Entry<TypePos, Annotation>>();
		for (Entry<TypePos, Annotation> entry : annotations.entrySet()) {
			sortedAnns.add(entry);
		}
		
		/*
		 * Sort the annotations by the end offset. This is useful in case the
		 * document changes (becomes shorter) before the annotations are set. In
		 * this case, the annotations which are too long/too late give a
		 * BadLocationException. If the annotations are not ordered, this may
		 * cause some of the annotations to go missing. If sorted, only the last
		 * ones (outside the document) get lost, which is acceptable.
		 */
		Collections.sort(sortedAnns, new Comparator<Entry<TypePos, Annotation>>() {
			@Override
			public int compare(Entry<TypePos, Annotation> o1, Entry<TypePos, Annotation> o2) {
				
				Position pos1 = o1.getKey().pos;
				Position pos2 = o2.getKey().pos;
				
				int end1 = pos1.getOffset() + pos1.getLength();
				int end2 = pos2.getOffset() + pos2.getLength();
				return end1 - end2;
			}
		});
		
		// preserve the sorted order
		Map<Annotation, Position> annMap = new LinkedHashMap<Annotation, Position>();
		
		for (Entry<TypePos, Annotation> entry : sortedAnns) {
			annMap.put(entry.getValue(), entry.getKey().pos);
		}
		
		return annMap;
	}
	
	private static class TypePos {
		private final String type;
		private final Position pos;
		
		public TypePos(String type, Position pos) {
			this.type = type;
			this.pos = pos;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((pos == null) ? 0 : pos.hashCode());
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
			TypePos other = (TypePos) obj;
			if (pos == null) {
				if (other.pos != null)
					return false;
			} else if (!pos.equals(other.pos))
				return false;
			if (type == null) {
				if (other.type != null)
					return false;
			} else if (!type.equals(other.type))
				return false;
			return true;
		}
	}
}
