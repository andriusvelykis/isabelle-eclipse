package isabelle.eclipse.ui.editors;

import isabelle.eclipse.core.text.IsabelleDocument;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModel;


public class IsabelleFileDocumentProvider extends TextFileDocumentProvider {

	private final Map<Object, IsabelleDocument> isabelleDocuments = new HashMap<Object, IsabelleDocument>();
	private final Map<Object, IAnnotationModel> annotationModels = new HashMap<Object, IAnnotationModel>();
	
	public IsabelleFileDocumentProvider() {
		super();
	}

	@Override
	public void connect(Object element) throws CoreException {
		super.connect(element);
//		System.out.println("Connecting isabelle document provider");
		IDocument baseDocument = getDocument(element);
		if (baseDocument != null) {
			IsabelleDocument document = new IsabelleDocument(baseDocument);
			isabelleDocuments.put(element, document);
			
			// create document partitioner: cannot do it via documentSetup extension point,
			// because the IsabelleDocument is created on top of the base document
			IDocumentPartitioner partitioner = new FastPartitioner(
					IsabellePartitions.createTheoryScanner(), IsabellePartitions.contentTypes());
			document.setDocumentPartitioner(IsabellePartitions.ISABELLE_PARTITIONING(), partitioner);
			partitioner.connect(document);
			
			
			IAnnotationModel annotationModel = createAnnotationModel(element);
			annotationModel.connect(document);
			annotationModels.put(element, annotationModel);
		}
	}
	
	private IAnnotationModel createAnnotationModel(Object element) {
		
		// TODO use annotation model factories, as in TextFileBufferManager#createAnnotationModel()?
		
		// check if we can find a resource for the given element
		IResource resource = EditorUtil.getResource(element);
		if (resource != null) {
			// resource located: use resource marker model
			return new ResourceMarkerAnnotationModel(resource);
		} else {
			// no resource available: use plain model
			return new AnnotationModel();
		}
	}

	@Override
	public void disconnect(Object element) {
		IsabelleDocument document = isabelleDocuments.remove(element);
		
		IAnnotationModel annotationModel = annotationModels.get(element);
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
			// use the base document for saving
			// it should be synced already via the listeners
			document = ((IsabelleDocument) document).base();
		}
		
		return super.createSaveOperation(element, document, overwrite);
	}

	@Override
	public IAnnotationModel getAnnotationModel(Object element) {
		return annotationModels.get(element);
	}
	
}
