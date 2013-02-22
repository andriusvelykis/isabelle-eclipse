package isabelle.eclipse.core.text

import isabelle.{Markup, Pretty, Protocol, Text, XML}
import isabelle.Command
import isabelle.Document.Snapshot


/**
 * A static factory to create various Isabelle annotations for a document snapshot.
 *
 * The annotations are created in a generic way using [[isabelle.eclipse.core.text.AnnotationInfo]]
 * data, so that users can later implement them as they wish
 * (e.g. via document annotations or markers, where applicable).
 *
 * This object encapsulates the calculation of the annotations, which types are given in
 * [[isabelle.eclipse.core.text.IsabelleAnnotation]].
 *
 * @author Andrius Velykis
 */
/* Annotation creation adapted from Rendering in Isabelle/jEdit */
object AnnotationFactory {

  /** Calculates abstract annotations for the given ranges in the snapshot. */
  def createAnnotations(snapshot: Snapshot, ranges: List[Text.Range]): List[AnnotationInfo] = {

    def allAnnotations(range: Text.Range) =
      createStatusAnnotations(snapshot, range) ++
        createMarkupAnnotations(snapshot, range) ++
        createMessageAnnotations(snapshot, range)

    // for each range calculate the different annotations
    // also remove duplicate annotations
    val annotations = ranges.map(allAnnotations).flatten.distinct

    // Sort the annotations by the end offset. This is useful in case the document changes
    // (becomes shorter) before the annotations are set. In this case, the annotations which
    // are too long/too late give a BadLocationException. If the annotations are not ordered,
    // this may cause some of the annotations to go missing. If sorted, only the last ones
    // (outside the document) get lost, which is acceptable.
    annotations.sortWith((a1, a2) => a1.range.stop < a2.range.stop)
  }

  private val statusInclude =
    Protocol.command_status_markup + Markup.WRITELN_MESSAGE + Markup.TRACING_MESSAGE +
      Markup.WARNING_MESSAGE + Markup.ERROR_MESSAGE + Markup.BAD + Markup.INTENSIFY// ++
//      active_include

  /** Creates command status annotations (e.g. unprocessed/outdated, etc.) */
  private def createStatusAnnotations(snapshot: Snapshot, range: Text.Range): Stream[AnnotationInfo] = {

    if (snapshot.is_outdated) Stream(AnnotationInfo(IsabelleAnnotation.STATUS_OUTDATED, range))
    else {
      for {
        Text.Info(r, result) <-
          snapshot.cumulate_markup[(Option[Protocol.Status], Option[IsabelleAnnotation])](
            range, (Some(Protocol.Status.init), None), Some(statusInclude), command_state =>
            {
              case (((Some(status), annType), Text.Info(_, XML.Elem(markup, _))))
                if (Protocol.command_status_markup(markup.name)) =>
                  (Some(Protocol.command_status(status, markup)), annType)
              case (_, Text.Info(_, XML.Elem(Markup(Markup.BAD, _), _))) =>
                (None, Some(IsabelleAnnotation.MARKUP_BAD))
              case (_, Text.Info(_, XML.Elem(Markup(Markup.INTENSIFY, _), _))) =>
                (None, Some(IsabelleAnnotation.MARKUP_INTENSIFY))
            })

        annType <- 
          (result match {
            case (Some(status), annHint) =>
              if (status.is_running) Some(IsabelleAnnotation.STATUS_UNFINISHED)
              else if (status.is_unprocessed) Some(IsabelleAnnotation.STATUS_UNPROCESSED)
              else annHint
            case (_, annHint) => annHint
          })
      } yield AnnotationInfo(annType, r)
    }
  }


  /** Creates markup annotations (e.g. ranges which should be highlighted in some way) */
  private def createMarkupAnnotations(snapshot: Snapshot, range: Text.Range): Stream[AnnotationInfo] = {

    val results =
      snapshot.select_markup[IsabelleAnnotation](range, Some(Set(Markup.TOKEN_RANGE)), _ =>
        {
          case Text.Info(_, XML.Elem(Markup(Markup.TOKEN_RANGE, _), _)) =>
            IsabelleAnnotation.MARKUP_TOKEN_RANGE
        })

    results map { case Text.Info(r, annType) => AnnotationInfo(annType, r) }
  }
  

  /** Creates message annotations (e.g. errors/warnings) */
  private def createMessageAnnotations(snapshot: Snapshot, range: Text.Range): Stream[AnnotationInfo] = {

    val msgMarkups = Set(Markup.WRITELN, Markup.WARNING, Markup.ERROR)
    
    val results =
      snapshot.select_markup[(IsabelleAnnotation, String)](range,
        // Markups in the snapshot that have associated messages and should be created as annotations.
        Some(msgMarkups), _ =>
        {
          case Text.Info(_, msg @ XML.Elem(Markup(name, _), body)) if msgMarkups.contains(name) => {
            val annType = name match {
              case Markup.WRITELN => IsabelleAnnotation.MESSAGE_WRITELN
              case Markup.WARNING => body match {
                case List(XML.Elem(Markup(Markup.LEGACY, _), _)) =>
                  IsabelleAnnotation.MESSAGE_LEGACY
                case _ => IsabelleAnnotation.MESSAGE_WARNING
              }
              case Markup.ERROR => IsabelleAnnotation.MESSAGE_ERROR
              // default case should not happen - only message markups are selected
            }

            val msgStr = Pretty.string_of(List(msg))
            (annType, msgStr)
          }
        })

    results map { case Text.Info(r, (annType, msg)) => AnnotationInfo(annType, r, Some(msg)) }
  }
  

  /** Calculates document ranges for the given commands. */
  def commandRanges(snapshot: Snapshot, commands: Set[Command]): List[Text.Range] = {

    val ranges = snapshot.node.command_range(0).collect {
      case (cmd, start) if commands.contains(cmd) => cmd.range + start
    }

    ranges.toList
  }
  
}

/**
 * Abstract representation of an annotation, carrying its type, position (range) and message.
 *
 * @author Andrius Velykis
 */
case class AnnotationInfo(val annType: IsabelleAnnotation,
                          val range: Text.Range,
                          val message: Option[String] = None)
