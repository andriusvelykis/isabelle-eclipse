package isabelle.eclipse.ui.editors

import scala.collection.JavaConverters._

import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.{IProgressMonitor, IStatus, Status}
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.source.{IAnnotationModel, IAnnotationModelExtension}
import org.eclipse.swt.widgets.Display

import isabelle.Command
import isabelle.Document.Snapshot
import isabelle.Text.Range
import isabelle.eclipse.core.text.{AnnotationFactory, AnnotationInfo}
import isabelle.eclipse.core.util.SerialSchedulingRule
import isabelle.eclipse.ui.IsabelleUIPlugin
import isabelle.eclipse.ui.util.SWTUtil.asyncExec


/**
 * Updater for Isabelle theory viewer annotations: creates annotations from the given
 * document snapshot.
 *
 * @author Andrius Velykis
 */
class TheoryViewerAnnotations(snapshot: => Option[Snapshot],
                              document: => IDocument,
                              annotationModel: => Option[IAnnotationModel],
                              markerResource: => Option[IResource] = None,
                              display: => Option[Display] = None) {
  
  private def allCommands: Set[Command] = 
    snapshot map (_.node.commands) getOrElse Set()

  val serialUpdateRule = new SerialSchedulingRule

  def updateAnnotations(changedCommands: Set[Command] = allCommands) {

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

      val annUpdater = new AnnotationUpdater(annModel, document, markerResource)
      annUpdater.updateAnnotations(changedRanges.asJava, annotations.asJava);
    }


  private def withAnnotationModel(f: IAnnotationModelExtension => Unit) {

    annotationModel match {
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
      
      // Only create new annotations if the Isabelle document snapshot is available
      // Note that old persistent markers are not deleted if present then
      val annsOpt = snapshot flatMap (s => createAnnotations(s, commands))

      // Set the annotations/markers in UI thread, otherwise getting
      // ConcurrentModificationException on the document positions
      // (e.g. when setting the annotations and repainting them at the same time)
      annsOpt foreach { case (anns, ranges) => asyncExec(display) { setAnnotations(anns, ranges) } }
      Status.OK_STATUS
    }
  }
  
}
