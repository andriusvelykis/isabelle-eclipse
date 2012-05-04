package isabelle.eclipse.editors;

import isabelle.eclipse.core.text.IsabelleDocument;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModel;


public class IsabelleFileDocumentProvider extends TextFileDocumentProvider {

	private final Map<Object, IsabelleDocument> isabelleDocuments = new HashMap<Object, IsabelleDocument>();
	private final Map<Object, ResourceMarkerAnnotationModel> annotationModels = new HashMap<Object, ResourceMarkerAnnotationModel>();
	
	public IsabelleFileDocumentProvider() {
		super();
	}

	@Override
	public void connect(Object element) throws CoreException {
		super.connect(element);
		System.out.println("Connecting isabelle document provider");
		IDocument baseDocument = getDocument(element);
		if (baseDocument != null) {
			IsabelleDocument document = new IsabelleDocument(baseDocument);
			isabelleDocuments.put(element, document);
			
			IResource markerResource = getMarkerResource(element);
			ResourceMarkerAnnotationModel annotationModel = new ResourceMarkerAnnotationModel(markerResource);
			annotationModel.connect(document);
			annotationModels.put(element, annotationModel);
		}
	}
	
	public static IResource getMarkerResource(Object element) {
		
		IFile file = getFile(element);
		if (file != null) {
			return file;
		}
		
		// otherwise use workspace root as the marker resource
		return ResourcesPlugin.getWorkspace().getRoot();
	}
	
	private static IFile getFile(Object element) {
		
		if (element instanceof IAdaptable) {
			return (IFile) ((IAdaptable) element).getAdapter(IFile.class);
		}
		
		return null;
	}

	@Override
	public void disconnect(Object element) {
		IsabelleDocument document = isabelleDocuments.remove(element);
		
		ResourceMarkerAnnotationModel annotationModel = annotationModels.get(element);
		if (annotationModel != null) {
			annotationModel.disconnect(document);
		}
		annotationModels.remove(element);
		
		super.disconnect(element);
	}

	@Override
	public IDocument getDocument(Object element) {
		
		IsabelleDocument document = isabelleDocuments.get(element);
		if (document != null) {
			return document;
		}
		
		return super.getDocument(element);
	}

	@Override
	protected DocumentProviderOperation createSaveOperation(Object element, IDocument document, boolean overwrite)
			throws CoreException {
		
		if (document instanceof IsabelleDocument) {
			// sync to the base one and use that
			((IsabelleDocument) document).syncToBase();
			document = ((IsabelleDocument) document).getBaseDocument();
		}
		
		return super.createSaveOperation(element, document, overwrite);
	}

	@Override
	public IAnnotationModel getAnnotationModel(Object element) {
		return annotationModels.get(element);
	}
	
}
