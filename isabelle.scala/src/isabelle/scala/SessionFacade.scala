
package isabelle.scala

import scala.collection.JavaConversions._
import scala.actors.Actor
import Actor._

import isabelle._


class SessionFacade (system: Isabelle_System) {

  val session = new Session(system)

  def getSession(): Session = session

  def addPhaseChangedActor(actor : SessionActor) {
    session.phase_changed += actor.getActor
  }

  def removePhaseChangedActor(actor : SessionActor) {
    session.phase_changed -= actor.getActor
  }

  def addRawMessagesActor(actor : SessionActor) {
    session.raw_messages += actor.getActor
  }

  def removeRawMessagesActor(actor : SessionActor) {
    session.raw_messages += actor.getActor
  }

  def addCommandsChangedActor(actor : SessionActor) {
    session.commands_changed += actor.getActor
  }

  def removeCommandsChangedActor(actor : SessionActor) {
    session.commands_changed += actor.getActor
  }

  def start(time : Long, args : Array[String]) {
    session.start(new Time(time), args.toList)
  }

  def edit(theoryName : String, text : String) {
    
    val all_edits = List( None, Some(List(Text.Edit.insert(0, text))) ).toList

    session.edit_version(all_edits.map((theoryName, _)))
  }

  def snapshot(theoryName : String, pendingEdits : Array[Text.Edit]) : Document.Snapshot =
    session.snapshot(theoryName, pendingEdits.toList);

  def commandStatus(state : Command.State) : DocumentStatusFacade =
    new DocumentStatusFacade(Isar_Document.command_status(state.status))

  def getInputDelay() : Long = session.input_delay.ms

  def getCompletion(text : String, activeBuffer : Boolean) : CompletionInfo = {
    val completion = session.current_syntax().completion
    completion.complete(text) match {
      case None => null
      case Some((word, cs)) =>
        val ds =
          cs.map(c => new DecodedCompletion(c, if (activeBuffer) system.symbols.decode(c) else c))
          //  .filter(c => c.getCompletion != word)
        if (ds.isEmpty) {
          null
        } else {
          new CompletionInfo(word, ds)
        }
    }
  }

  class CompletionInfo(word : String, completions : List[DecodedCompletion]) {
    def getWord() = word
    def getCompletions() : java.util.List[DecodedCompletion] = completions
  }
  
  class DecodedCompletion(completion : String, decoded : String) {
    def getCompletion() = completion
    def getDecoded() = decoded
  }

//  def markupSnapshot(theoryName : String, pendingEdits : Array[Text.Edit]) : Document.Snapshot = {
//    val execList = session.current_state.commands.map(commandMap => (commandMap._1, (-1 * commandMap._1).asInstanceOf[Document.Exec_ID]))
//    println("New exec list: " + execList)
//    val versionId = session.current_state.history.undo_list.head.version.get_finished.id
//    println("Target version ID: " + versionId)
//    session.global_state.change_yield(_.assign(versionId, execList.toList))
////    session.current_state.assign(versionId, execList.toList)
//    session.snapshot(theoryName, pendingEdits.toList);
//  }
//
//
//}
//
//class LocalSessionFacade(system : Isabelle_System) extends SessionFacade (
//  new Session(system) {
//    override def edit_version(edits: List[Document.Edit_Text]) {
//      throw new UnsupportedOperationException("Local session does not support process-based editing")
//    }
//  }) {
//
//  private val localState = new Volatile(Document.State.init)
//
//  private def textEdits(edits: List[Document.Edit_Text]) {
//    val previous = localState.peek().history.tip.version
//    val result = new Finished_Future(Thy_Syntax.text_edits(getSession(), previous.join, edits))
//    val change = localState.change_yield(_.extend_history(previous, edits, result))
//
//    handleChange(change)
//  }
//
//  def handleChange(change: Document.Change)
//  //{{{
//  {
//    val previous = change.previous.get_finished
//    val (node_edits, version) = change.result.get_finished
//
//    var former_assignment = localState.peek().the_assignment(previous).get_finished
//    for {
//      (name, Some(cmd_edits)) <- node_edits
//      (prev, None) <- cmd_edits
//      removed <- previous.nodes(name).commands.get_after(prev)
//    } former_assignment -= removed
//
//
////    val id_edits =
////      node_edits map {
////        case (name, None) => (name, None)
////        case (name, Some(cmd_edits)) =>
////          val ids =
////            cmd_edits map {
////              case (c1, c2) =>
////                val id1 = c1.map(_.id)
////                val id2 =
////                  c2 match {
////                    case None => None
////                    case Some(command) =>
////                      if (localState.peek().lookup_command(command.id).isEmpty) {
////                        localState.change(_.define_command(command))
////                      }
////                      Some(command.id)
////                  }
////                (id1, id2)
////            }
////          (name -> Some(ids))
////      }
//
//    val commands = new ListBuffer[Document.Command_ID]()
//    val id_edits =
//      node_edits map {
//        case (name, None) => (name, None)
//        case (name, Some(cmd_edits)) =>
//          val ids =
//            cmd_edits map {
//              case (c1, c2) =>
//                val id1 = c1.map(_.id)
//                val id2 =
//                  c2 match {
//                    case None => None
//                    case Some(command) =>
//                      if (localState.peek().lookup_command(command.id).isEmpty) {
//                        localState.change(_.define_command(command))
//                      }
//                      commands += command.id
//                      Some(command.id)
//                  }
//                (id1, id2)
//            }
//          (name -> Some(ids))
//      }
//    localState.change(_.define_version(version, former_assignment))
//
//    val edits = commands.map(commandId => (commandId, commandId.asInstanceOf[Document.Exec_ID]))
//
//    // assign with no edits (no round-trip to the prover)
//    localState.change_yield(_.assign(version.id, edits.toList))
//    getSession.assignments.event(Session.Assignment)
//  }
//
//  override def edit(theoryName : String, text : String) {
//    val all_edits = List( Some(List(Text.Edit.insert(0, text))) ).toList
//    textEdits(all_edits.map((theoryName, _)))
//  }
//
//  override def snapshot(theoryName : String, pendingEdits : Array[Text.Edit]) : Document.Snapshot =
//    localState.peek().snapshot(theoryName, pendingEdits.toList)

}
