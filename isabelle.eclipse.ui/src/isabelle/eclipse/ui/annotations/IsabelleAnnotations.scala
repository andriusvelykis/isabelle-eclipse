package isabelle.eclipse.ui.annotations

import java.util.{LinkedHashMap, Map => JavaMap}

import scala.language.implicitConversions
import scala.collection.immutable.{Map, Seq}

import org.eclipse.jface.text.{IDocument, ISynchronizable, Position}
import org.eclipse.jface.text.source.{Annotation, AnnotationModel, IAnnotationModelExtension}

import isabelle.Text.Range
import isabelle.eclipse.core.text.{AnnotationInfo, IsabelleAnnotation}
import isabelle.eclipse.ui.internal.IsabelleUIPlugin


/**
 * An Annotation Model mixin trait that caches annotation definitions. Creates document annotations
 * based on the given abstract annotation definitions.
 * 
 * The annotation configuration (abstract-concrete mappings) come from
 * `IsabelleAnnotationConstants`.
 * 
 * This trait allows replacing annotations for a given changed range. Before adding new annotations,
 * it checks if there are corresponding existing annotations. In that case, the existing ones are
 * reused and updates are lighter on the UI.
 * 
 * Note: currently there is no synchronisation, so use it from the UI thread
 * 
 * @author Andrius Velykis
 */
trait IsabelleAnnotations extends IAnnotationModelExtension {

  val annotationModelKey = IsabelleUIPlugin.plugin.pluginId + ".isabelle-annotations" 

  def document: IDocument

  def documentRange = Range(0, document.getLength)

  def annotationModel = attachedAnnotationModel(this, annotationModelKey)

  /**
   * Supported annotation types (mapping to actual annotation definitions).
   * 
   * Subclasses may override if rendering only part of annotations as Eclipse annotations
   * (e.g. the other part as markers)
   */
  def annotationTypes: Map[IsabelleAnnotation, String] = IsabelleAnnotationConstants.annotationTypes


  private var modelAnnDefs: Seq[AnnotationInfo] = Seq()

  private var existingAnns: Map[AnnotationInfo, Annotation] = Map()

  def replaceAnnotationsRange(anns: Seq[AnnotationInfo], ranges: Option[Seq[Range]] = None) {

    val docRange = documentRange

    // restrict the ranges to the document
    val restrictRanges = ranges.map {rs => (rs map docRange.try_restrict).flatten }
    val changedRanges = restrictRanges getOrElse List(docRange)

    val currentAnns = modelAnnDefs

    // get annotations that fall within the changed ranges
    val changedAnns = currentAnns.filter {ann => changedRanges exists ann.range.overlaps }

    // find changed annotations which are not replaced (will be deleted)
    val deleteAnns = changedAnns diff anns
    val newAnns = anns diff changedAnns

    // get annotations outside the document (will be deleted)
    val outsideAnns = currentAnns.filter {_.range.start > docRange.stop}
    val allDeleteAnns = deleteAnns ++ outsideAnns

    val afterAnns = (currentAnns diff allDeleteAnns) ++ newAnns
    this.modelAnnDefs = afterAnns

    doReplaceAnnotations(docRange, newAnns, allDeleteAnns)
  }

  protected def doReplaceAnnotations(docRange: Range,
                                     newAnnDefs: Seq[AnnotationInfo],
                                     deleteAnnDefs: Seq[AnnotationInfo]) {

    val (newAnns, addAnnMap) = createAnnotations(docRange, newAnnDefs)

    val currentAnns = existingAnns
    val deleteAnns = (deleteAnnDefs map currentAnns.get).flatten

    val afterAnns = (currentAnns -- deleteAnnDefs) ++ newAnns
    this.existingAnns = afterAnns

    // update the annotations in the model
    annotationModel.replaceAnnotations(deleteAnns.toArray, addAnnMap)
  }


  private def createAnnotations(docRange: Range,
                                newAnnDefs: Seq[AnnotationInfo]): 
      (Map[AnnotationInfo, Annotation], JavaMap[Annotation, Position]) = {

    val eAnns = newAnnDefs.map { annDef => createAnnotation(docRange, annDef) map ((annDef, _)) }

    val validAnns = eAnns.flatten
    val newAnns = validAnns.map { case (annDef, (ann, _)) => (annDef, ann) }

    // preserve the original order
    val annMap = new LinkedHashMap[Annotation, Position]
    validAnns.foreach { case (_, (ann, pos)) => annMap.put(ann, pos) }

    (newAnns.toMap, annMap)
  }

  private def createAnnotation(docRange: Range,
                               ann: AnnotationInfo): Option[(Annotation, Position)] =
    // avoid creating annotation if its type is not available
    annotationTypes.get(ann.annType).flatMap { annType =>

      // try to restrict the range to document limits
      // (ignore annotation if the range is invalid (e.g. outside the max range))
      docRange.try_restrict(ann.range).map { annRange =>

        val eAnn = new Annotation(false)
        eAnn.setType(annType)
        eAnn.setText(ann.message.orNull)

        (eAnn, annRange)
      }
    }

  private implicit def toPosition(range: Range): Position =
    new Position(range.start, range.length)


  /**
   * Retrieves an annotation model attachment. If one is not available, it is created.
   */
  private def attachedAnnotationModel(baseModel: IAnnotationModelExtension,
                                      key: AnyRef): IAnnotationModelExtension = {

    val model = Option(baseModel.getAnnotationModel(key))
    model match {
      case Some(m: IAnnotationModelExtension) => m

      case Some(unknown) => 
        throw new IllegalStateException("Unsupported annotation model: " + unknown.getClass)

      case None => {

        val newModel = new AnnotationModel
        baseModel match {
          case syncModel: ISynchronizable => newModel.setLockObject(syncModel.getLockObject)
        }

        baseModel.addAnnotationModel(key, newModel)
        newModel
      }
    }
  }
  
}
