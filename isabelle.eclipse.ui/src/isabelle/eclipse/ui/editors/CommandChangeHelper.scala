package isabelle.eclipse.ui.editors

import scala.actors.Actor._

import isabelle.{Command, Session}
import isabelle.Document.Snapshot
import isabelle.Text.Range
import isabelle.eclipse.core.text.DocumentModel
import isabelle.eclipse.core.util.{LoggingActor, SessionEvents}

/**
 * A listener wrapper for Isabelle session command change events.
 *
 * Reacts to command changes and calculates the affected document ranges, which are then pushed
 * as handler notifications.
 *
 * @author Andrius Velykis
 */
class CommandChangeHelper(docModel: DocumentModel,
                          fireInit: Boolean = false)(
                              handler: Option[List[Range]] => Unit) extends SessionEvents {

  // When commands change (e.g. results from the prover), notify the handler about changed ranges.
  /** Subscribe to commands change session events */
  override protected def sessionEvents(session: Session) = List(session.commands_changed)

  /** When the session is initialised, notify about "all changed" if enabled */
  override protected def sessionInit(session: Session) =
    if (fireInit) notifyCommandsChanged(None)

  /** The actor to react to session events */
  override protected val sessionActor = LoggingActor {
    loop {
      react {
        case changed: Session.Commands_Changed => {

          // avoid updating if commands are from a different document
          if (changed.nodes contains docModel.name) {
            notifyCommandsChanged(Some(changed.commands))
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

  private def notifyCommandsChanged(changedCmds: Option[Set[Command]]) {
    val ranges = changedRanges(docModel.snapshot, changedCmds)
    handler(ranges)
  }

  @volatile private var lastCommandCount = 0
  @volatile private var lastSnapshotOutdated = true

  private def changedRanges(snapshot: Snapshot,
                            changedCmds: Option[Set[Command]]): Option[List[Range]] = {

    val snapshotCmds = snapshot.node.commands
    val currentCommandCount = snapshotCmds.size
    val commands = changedCmds flatMap { cmds =>
      if (currentCommandCount > lastCommandCount || lastSnapshotOutdated) {
        // More commands in the snapshot than was previously - update annotations for the
        // whole snapshot. This is necessary because parsing can happen slowly and commands
        // appear delayed in the snapshot.

        // This is a workaround because parsing events are not firing notifications, so we
        // manually check if we need updating. We update if the last snapshot was outdated,
        // or new commands were added (e.g. via parsing).
        None
      } else {
        // Only use commands that are in the snapshot.
        Some(cmds intersect snapshotCmds)
      }
    }

    lastCommandCount = currentCommandCount
    lastSnapshotOutdated = snapshot.is_outdated

    commands map { cmds =>
      if (cmds.isEmpty) {
        Nil
      } else {

        // get the ranges occupied by the changed commands
        // and refresh the view/recalculate annotations for them afterwards
        val cmdRanges = commandRanges(snapshot, cmds)

        // merge overlapping/adjoining ranges
        val ranges = mergeRanges(cmdRanges)
        ranges
      }
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

}
