package isabelle.eclipse.ui.editors;

import isabelle.Text.Range;
import isabelle.eclipse.core.text.AnnotationInfo;
import isabelle.eclipse.core.text.IsabelleAnnotation;
import isabelle.eclipse.ui.IsabelleUIPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import scala.Option;
import scala.Some;

import static isabelle.eclipse.ui.editors.IsabelleAnnotationConstants.*;

/**
 * A class to create annotations and markers based on the given abstract annotation definitions. The
 * annotation configuration (abstract-concrete mappings) come from
 * {@link IsabelleAnnotationConstants}.
 * <p>
 * Before adding new annotations/markers, the class checks if there are corresponding existing
 * annotations/markers. In that case, the existing ones are reused and updates are lighter.
 * </p>
 * 
 * @author Andrius Velykis
 */
public class AnnotationUpdater {

	/**
	 * A child annotation model is used referenced with this key.
	 */
	private static final Object ANNOTATION_MODEL_KEY = new Object();
	
	private final IAnnotationModelExtension baseAnnotationModel;
	private final IDocument document;
	private final IResource markerResource;
	
	public AnnotationUpdater(IAnnotationModelExtension baseAnnotationModel, IDocument document,
			Option<IResource> markerResource) {
		this.baseAnnotationModel = baseAnnotationModel;
		this.document = document;
		this.markerResource = markerResource.isDefined() ? markerResource.get() : null;
	}
	
	/**
	 * Sets the stored annotations onto the editor/resource. Does not recreate annotations/markers
	 * if the same ones already exist.
	 * 
	 * @param changedRanges
	 * @param newAnnotations
	 */
	public void updateAnnotations(List<Range> changedRanges, List<AnnotationInfo> newAnnotations) {
		
		// Use a separate attached annotation model for Isabelle annotations
		AnnotationModel annotationModel = getAnnotationModel(baseAnnotationModel, ANNOTATION_MODEL_KEY);
		
		/*
		 * Filter the keys of annotations as defined in IsabelleAnnotationConstants - only the types
		 * that have concrete mappings will be used. If a resource is available to create markers
		 * on, they are creates as defined in the configuration. Duplicate annotations are then
		 * avoided.
		 */
		Set<IsabelleAnnotation> annTypes = EnumSet.copyOf(ANNOTATION_KEYS.keySet());
		Set<IsabelleAnnotation> markerTypes;
		if (markerResource != null) {
			// set markers - and remove corresponding types from annotations to avoid duplication
			markerTypes = EnumSet.copyOf(MARKER_KEYS.keySet()); 
			annTypes.removeAll(markerTypes);
		} else {
			markerTypes = EnumSet.noneOf(IsabelleAnnotation.class);
		}
		
		// restrict the ranges to document length
		Range maxRange = new Range(0, Math.max(document.getLength() - 1, 0));
		
		// get only the annotation definitions (exclude undefined/markers)
		List<AnnotationInfo> newAnns = filterAnnotations(annTypes, newAnnotations);
		
		// get the existing annotations in the changed ranges
		List<Annotation> changedAnns = getChangedAnnotations(annotationModel, changedRanges);
		// filter the existing to avoid deleting if they match one of the new ones
		List<Annotation> deleteAnns = filterExistingAnnotations(annotationModel, changedAnns, newAnns);

		Annotation[] deleteAnnsArray = deleteAnns.toArray(new Annotation[deleteAnns.size()]);
		// create the annotation objects to put into the annotation model
		Map<Annotation, Position> addAnns = getAnnotationMap(maxRange, newAnns);
		
		if (markerResource != null) {
			// resource is available for markers - create them
			// Note: if marker resource is not available, corresponding annotations are shown
			
			// get only the marker definitions (exclude undefined/annotations)
			List<AnnotationInfo> newMarkers = filterAnnotations(markerTypes, newAnnotations);
			
			// get existing markers in the changed ranges
			List<IMarker> changedMarkers = getChangedMarkers(markerResource, changedRanges);
			// filter the existing to avoid deleting if they match one of the new ones
			List<IMarker> deleteMarkers = filterExistingMarkers(changedMarkers, newMarkers);
			// create the marker objects
			setMarkers(markerResource, maxRange, deleteMarkers, newMarkers);
		}
		
		// update the annotations in the model
		annotationModel.replaceAnnotations(deleteAnnsArray, addAnns);
	}
	
	/**
	 * Retrieves an annotation model attachment. If one is not available, it is created.
	 * 
	 * @param baseModel
	 * @param key
	 * @return
	 */
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
	
	private List<AnnotationInfo> filterAnnotations(Set<IsabelleAnnotation> types, List<AnnotationInfo> anns) {
		
		List<AnnotationInfo> filtered = new ArrayList<AnnotationInfo>();
		
		for (AnnotationInfo ann : anns) {
			if (types.contains(ann.annType())) {
				filtered.add(ann);
			}
		}
		
		return filtered;
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
			Position pos = getPosition(range);
			for (Iterator<?> it = annotationModel.getAnnotationIterator(
					pos.getOffset(), pos.getLength(), true, true); 
					it.hasNext(); ) {
				
				changedAnns.add((Annotation) it.next());
			}
		}
		
		return new ArrayList<Annotation>(changedAnns);
	}
	
	private List<Annotation> filterExistingAnnotations(IAnnotationModel annotationModel, 
			List<Annotation> existingAnns, List<AnnotationInfo> newAnns) {
		
		List<Annotation> deleteAnns = new ArrayList<Annotation>();
		
		// check if annotation with the matching info is in the new state
		for (Annotation ann : existingAnns) {
			Position annPos = annotationModel.getPosition(ann);
			
			IsabelleAnnotation existingType = ANNOTATION_TYPES.get(ann.getType());
			if (existingType != null) {

				// remove matching annotations from the new ones
				AnnotationInfo existingAnn = new AnnotationInfo(existingType, getRange(annPos), Some.apply(ann.getText()));
				boolean newAnnFound = newAnns.removeAll(Arrays.asList(existingAnn));
				
				if (newAnnFound) {
					// found a corresponding annotation in the new ones, so keep the existing one
					continue;
				}
			}
			
			// did not match with a corresponding annotation in the new state, so delete it
			deleteAnns.add(ann); 
		}
		
		return deleteAnns;
	}
	
	private Range getRange(Position pos) {
		return new Range(pos.getOffset(), pos.getOffset() + pos.getLength());
	}
	
	private Position getPosition(Range range) {
		return new Position(range.start(), range.stop() - range.start());
	}
	
	/**
	 * Creates the annotation objects and positions from abstract annotation definition.
	 * 
	 * @param maxRange
	 *            maximum range to restrict the position (to avoid annotations outside the document)
	 * @param anns
	 * @return
	 */
	private Map<Annotation, Position> getAnnotationMap(Range maxRange, List<AnnotationInfo> anns) {
		
		// preserve the sorted order
		Map<Annotation, Position> annMap = new LinkedHashMap<Annotation, Position>();
		
		for (AnnotationInfo ann : anns) {
			
			Annotation annotation = new Annotation(false);
			String annKey = ANNOTATION_KEYS.get(ann.annType());
			annotation.setType(annKey);
			
			annotation.setText(ann.message().isDefined() ? ann.message().get() : null);
			
			// try to restrict the range to document limits
			Option<Range> fixedRangeOpt = maxRange.try_restrict(ann.range());
			if (fixedRangeOpt.isEmpty()) {
				// invalid range (e.g. outside the max range)
				// so ignore the annotation altogether
				continue;
			}
			
			Range fixedRange = fixedRangeOpt.get();
			Position position = getPosition(fixedRange);
			
			annMap.put(annotation, position);
		}
		
		return annMap;
	}
	
	private List<IMarker> getChangedMarkers(IResource markerResource, List<Range> changeRange) {
		
		List<IMarker> allMarkers = new ArrayList<IMarker>();
		try {
			for (String markerType : MARKERS) {
				allMarkers.addAll(Arrays.asList(
						markerResource.findMarkers(markerType, false, IResource.DEPTH_ZERO)));
			}
		} catch (CoreException e) {
			IsabelleUIPlugin.log(e.getLocalizedMessage(), e);
		}
		
		if (changeRange.isEmpty()) {
			// use all markers
			return allMarkers;
		}

		List<IMarker> changedMarkers = new ArrayList<IMarker>();

		// find markers that overlap with the given ranges, they are considered
		// changed and will be examined
		for (IMarker marker : allMarkers) {
			Range markerRange = getMarkerRange(marker);
			
			for (Range range : changeRange) {
				if (markerRange.overlaps(range)) {
					changedMarkers.add(marker);
					break;
				}
			}
		}
		
		return changedMarkers;
	}
	
	private Range getMarkerRange(IMarker marker) {
		return new Range(marker.getAttribute(IMarker.CHAR_START, 0), 
						 marker.getAttribute(IMarker.CHAR_END, Integer.MAX_VALUE));
	}
	
	private List<IMarker> filterExistingMarkers(List<IMarker> markers,
			List<AnnotationInfo> newMarkers) {
		
		List<IMarker> deleteMarkers = new ArrayList<IMarker>();
		
		for (IMarker marker : markers) {
			
			try {
				
				MarkerKey markerKey = new MarkerKey(marker.getType(), marker.getAttribute(IMarker.SEVERITY, -1));
				IsabelleAnnotation existingType = MARKER_TYPES.get(markerKey);
				
				if (existingType != null) {

					// remove matching markers from the new ones
					AnnotationInfo existingMarker = new AnnotationInfo(existingType,
							getMarkerRange(marker), Option.apply(marker.getAttribute(IMarker.MESSAGE, null)));
					boolean newMarkerFound = newMarkers.removeAll(Arrays.asList(existingMarker));
					
					if (newMarkerFound) {
						// found a corresponding marker in the new ones, so keep the existing one
						continue;
					}
				}
				
			} catch (CoreException e) {
				IsabelleUIPlugin.log(e.getLocalizedMessage(), e);
			}
			
			// did not match with a corresponding marker in the new state, so delete it
			deleteMarkers.add(marker);
		}
		
		return deleteMarkers;
	}
	
	private void setMarkers(final IResource markerResource, final Range maxRange,
			final List<IMarker> deleteMarkers,
			final List<AnnotationInfo> addMarkers) {
		
		// add new markers
		IWorkspaceRunnable r = new IWorkspaceRunnable() {
			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				
				for (IMarker marker : deleteMarkers) {
					marker.delete();
				}
				
				for (AnnotationInfo markerInfo : addMarkers) {
					MarkerKey markerKey = MARKER_KEYS.get(markerInfo.annType());
					IMarker marker = markerResource.createMarker(markerKey.getKey());
					marker.setAttributes(getMarkerAttributes(maxRange, markerInfo, markerKey));
				}
			}
		};

		try {
			markerResource.getWorkspace().run(r, null, IWorkspace.AVOID_UPDATE, null);
		} catch (CoreException ce) {
			IsabelleUIPlugin.log(ce.getMessage(), ce);
		}
	}
	
	private Map<String, Object> getMarkerAttributes(Range maxRange, AnnotationInfo ann, MarkerKey markerKey) {
		
		// restrict the range to avoid exceeding the document range
		Option<Range> fixedRangeOpt = maxRange.try_restrict(ann.range());
		Range fixedRange;
		if (fixedRangeOpt.isEmpty()) {
			// invalid range (e.g. outside the max range)
			// do not ignore, but better display it at (0, 0)
			fixedRange = new Range(0, 0);
		} else {
			fixedRange = fixedRangeOpt.get();
		}
		
		Map<String, Object> markerAttrs = new HashMap<String, Object>();
		
		markerAttrs.put(IMarker.SEVERITY, markerKey.getSeverity());
		markerAttrs.put(IMarker.CHAR_START, fixedRange.start());
		markerAttrs.put(IMarker.CHAR_END, fixedRange.stop());
		try {
			if (document != null) {
				int line = document.getLineOfOffset(fixedRange.start()) + 1;
				markerAttrs.put(IMarker.LOCATION, "line " + line);
				markerAttrs.put(IMarker.LINE_NUMBER, line);
			}
		} catch (BadLocationException ex) {
			// ignore
		}
		
		markerAttrs.put(IMarker.MESSAGE, ann.message().isDefined() ? ann.message().get() : null);
		
		return markerAttrs;
	}
	
}
