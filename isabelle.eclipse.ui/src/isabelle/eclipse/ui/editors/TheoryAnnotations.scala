package isabelle.eclipse.ui.editors

import scala.actors.Actor._
import scala.collection.JavaConverters._

import org.eclipse.core.runtime.{IProgressMonitor, IStatus, Status}
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.jface.text.source.IAnnotationModelExtension

import isabelle.Command
import isabelle.Document.Snapshot
import isabelle.Session
import isabelle.Text.Range
import isabelle.eclipse.core.text.{AnnotationFactory, AnnotationInfo, DocumentModel}
import isabelle.eclipse.core.util.{LoggingActor, SerialSchedulingRule, SessionEvents}
import isabelle.eclipse.ui.IsabelleUIPlugin
import isabelle.eclipse.ui.util.SWTUtil.runInUI


/**
 * Updater for Isabelle theory editor annotations: tracks changes from the prover,
 * creates annotations and triggers update jobs.
 * 
 * @author Andrius Velykis
 */
class TheoryAnnotations(editor: TheoryEditor) extends SessionEvents {

  // When commands change (e.g. results from the prover), update the annotations accordingly.
  /** Subscribe to commands change session events */
  override protected def sessionEvents(session: Session) = List(session.commands_changed)
  
  /** When the session is initialised, update all annotations from scratch */
  override protected def sessionInit(session: Session) = updateAnnotations()
  
  /** The actor to react to session events */
  override protected val sessionActor = LoggingActor {
    loop {
      react {
        case changed: Session.Commands_Changed => {

          docModel foreach { model =>
            
            // avoid updating annotations if commands are from a different document
            if (changed.nodes contains model.name) {
              updateAnnotations(changed.commands)
            }
          }
        }
      }
    }
  }
  
  def init() {
    initSessionEvents()
  }
  
  def dispose() {
    disposeSessionEvents()
  }
  
  private def docModel: Option[DocumentModel] = editor.isabelleModel
  
  private def allCommands: Set[Command] = 
    (docModel map (_.snapshot.node.commands)) getOrElse Set()

  val serialUpdateRule = new SerialSchedulingRule

  private def updateAnnotations(changedCommands: Set[Command] = allCommands) {

    val updateJob = new AnnotationUpdateJob(changedCommands)

    // synchronise the jobs - only a single update can be running
    updateJob.setRule(serialUpdateRule)
    // top priority to give quick feedback to the user
    updateJob.setPriority(Job.INTERACTIVE)
//    updateJob.setPriority(Job.DECORATE)

    // system job - do not display progress in the UI
    updateJob.setSystem(true)

    updateJob.schedule()
  }

  @volatile private var lastCommandCount = 0
  @volatile private var lastSnapshotOutdated = true

  private def createAnnotations(snapshot: Snapshot,
                                changedCmds: Set[Command])
      : Option[(List[AnnotationInfo], List[Range])] = {

    val snapshotCmds = snapshot.node.commands
    val currentCommandCount = snapshotCmds.size
    val commands =
      if (currentCommandCount > lastCommandCount || lastSnapshotOutdated) {
        // More commands in the snapshot than was previously - update annotations for the
        // whole snapshot. This is necessary because parsing can happen slowly and commands
        // appear delayed in the snapshot.

        // This is a workaround because parsing events are not firing notifications, so we
        // manually check if we need updating. We update if the last snapshot was outdated,
        // or new commands were added (e.g. via parsing).
        snapshotCmds
      } else {
        // Only use commands that are in the snapshot.
        changedCmds intersect snapshotCmds
      }

    lastCommandCount = currentCommandCount
    lastSnapshotOutdated = snapshot.is_outdated

    if (commands.isEmpty) {
      None
    } else {

      // get the ranges occupied by the changed commands
      // and recalculate annotations for them afterwards
      val cmdRanges = commandRanges(snapshot, commands)

      // merge overlapping/adjoining ranges
      val ranges = mergeRanges(cmdRanges)

      val annotations = AnnotationFactory.createAnnotations(snapshot, ranges)
      Some(annotations, ranges)
    }
  }

  
  /**
   * Calculates document ranges for the given commands.
   */
  private def commandRanges(snapshot: Snapshot, commands: Set[Command]): List[Range] = {

    val ranges = snapshot.node.command_range(0).collect {
      case (cmd, start) if commands.contains(cmd) => cmd.range + start
    }

    ranges.toList
  }
  
  
  /**
   * Merges overlapping/adjoined ranges.
   */
  private def mergeRanges(rangesTr: TraversableOnce[Range]): List[Range] = {

    // sort the ranges just in case
    val ranges = rangesTr.toList.sorted(Range.Ordering)

    def merge(pending: List[Range], acc: List[Range]): List[Range] = pending match {
      case Nil => acc

      case single :: Nil => single :: acc

      case r1 :: r2 :: rs => if (r2.start - r1.stop <= 1) {
        // either the ranges overlap, or the gap between them is too small
        // merge and continue
        val merged = Range((r1.start min r2.start), (r1.stop max r2.stop))
        merge(merged :: rs, acc)
      } else {
        // not overlapping ranges - accumulate first and continue to the next one
        merge(r2 :: rs, r1 :: acc)
      }
    }

    merge(ranges, Nil).reverse
  }


  private def setAnnotations(annotations: List[AnnotationInfo], changedRanges: List[Range]) =
    withAnnotationModel { annModel =>

      val document = editor.document
      val markerResource = Option(EditorUtil.getResource(editor.getEditorInput))

      val annUpdater = new AnnotationUpdater(annModel, document, markerResource)
      annUpdater.updateAnnotations(changedRanges.asJava, annotations.asJava);
    }


  private def withAnnotationModel(f: IAnnotationModelExtension => Unit) {

    val annModel = Option(editor.getDocumentProvider.getAnnotationModel(editor.getEditorInput))

    annModel match {
      case Some(modern: IAnnotationModelExtension) => f(modern)
      case Some(old) =>
        IsabelleUIPlugin.log("Obsolete annotation model is used: " + old.getClass, null)
      case _ => {}
    }
  }
  
    
  /**
   * A private job to update annotations in a separate thread.
   * 
   * First calculates the annotations, then sets them in the UI thread.
   *
   * @author Andrius Velykis
   */
  private class AnnotationUpdateJob(commands: Set[Command])
      extends Job("Updating theory annotations") {

    override def run(monitor: IProgressMonitor): IStatus = {
      
      // Only create new annotations if the Isabelle document model is available
      // Note that old persistent markers are not deleted if present then
      val annsOpt = docModel flatMap (m => createAnnotations(m.snapshot, commands))

      // Set the annotations/markers in UI thread, otherwise getting
      // ConcurrentModificationException on the document positions
      // (e.g. when setting the annotations and repainting them at the same time)
      annsOpt foreach { case (anns, ranges) => runInUI(editor) { setAnnotations(anns, ranges) } }
      Status.OK_STATUS
    }
  }
  
}
