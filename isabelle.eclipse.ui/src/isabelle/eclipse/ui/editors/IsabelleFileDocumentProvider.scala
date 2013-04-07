package isabelle.eclipse.ui.editors

import scala.collection.mutable

import org.eclipse.core.runtime.CoreException
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.source.{AnnotationModel, IAnnotationModel}
import org.eclipse.ui.editors.text.TextFileDocumentProvider
import org.eclipse.ui.editors.text.TextFileDocumentProvider.DocumentProviderOperation
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModel

import isabelle.eclipse.core.text.IsabelleDocument
import isabelle.eclipse.ui.annotations.{IsabelleAnnotations, IsabelleMarkerAnnotations}


/**
 * A file provider for Isabelle theory files that wraps each document into IsabelleDocument,
 * which performs conversion of special characters.
 * 
 * Also provides annotation models with special support for Isabelle annotation replacement.
 * 
 * @author Andrius Velykis
 */
class IsabelleFileDocumentProvider extends TextFileDocumentProvider {

  private val isabelleDocuments: mutable.Map[Any, IsabelleDocument] = mutable.Map()
  private val annotationModels: mutable.Map[Any, IAnnotationModel with IsabelleAnnotations] = 
    mutable.Map()


  @throws[CoreException]
  override def connect(element: Any) {
    super.connect(element)

    Option(getDocument(element)) foreach { baseDocument =>
      {
        val document = new IsabelleDocument(baseDocument) with IsabellePartitions
        val annotationModel = createAnnotationModel(element, document)
        annotationModel.connect(document)

        isabelleDocuments += (element -> document)
        annotationModels += (element -> annotationModel)
      }
    }
  }

  private def createAnnotationModel(element: Any,
                                    doc: IDocument): IAnnotationModel with IsabelleAnnotations = {
    // TODO use annotation model factories, as in TextFileBufferManager#createAnnotationModel()?

    // check if we can find a resource for the given element
    Option(EditorUtil.getResource(element)) match {

      // resource located: use resource marker model
      case Some(res) => new ResourceMarkerAnnotationModel(res) with IsabelleMarkerAnnotations {
        override val document = doc
        override val markerResource = res
      }

      // no resource available: use plain model
      case None => new AnnotationModel with IsabelleAnnotations {
        override val document = doc
      }
    }
  }


  override def disconnect(element: Any) {
    val document = isabelleDocuments.remove(element)
    val annotationModel = annotationModels.remove(element)

    (annotationModel, document) match {
      case (Some(model), Some(document)) => model.disconnect(document)
      case _ =>
    }

    // dispose the IsabelleDocument to disconnect sync from base document
    document foreach (_.dispose())

    super.disconnect(element)
  }


  override def getDocument(element: Any): IDocument =
    isabelleDocuments.get(element) getOrElse super.getDocument(element)

  override def getAnnotationModel(element: Any): IAnnotationModel with IsabelleAnnotations =
    annotationModels.get(element).orNull


  @throws[CoreException]
  override def createSaveOperation(element: Any,
                                   document: IDocument,
                                   overwrite: Boolean): DocumentProviderOperation = {

    val saveDoc = document match {
      // use the base document for saving: it should be synced already via the listeners
      case isaDocument: IsabelleDocument => isaDocument.base
      case _ => document
    }

    super.createSaveOperation(element, saveDoc, overwrite)
  }

}
