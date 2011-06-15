
package isabelle.scala

import scala.collection.mutable

import isabelle._

class DocumentModel(val sessionFacade: SessionFacade, val thy_name: String) {

  private val session = sessionFacade.getSession

  object pending_edits  // owned by Swing thread
  {
    private val pending = new mutable.ListBuffer[Text.Edit]
    def snapshot(): List[Text.Edit] = pending.toList

    def flush(more_edits: Option[List[Text.Edit]]*)
    {
      val edits = snapshot();
      pending.clear

      val all_edits =
        if (edits.isEmpty) more_edits.toList
        else Some(edits) :: more_edits.toList
      if (!all_edits.isEmpty) session.edit_version(all_edits.map((thy_name, _)))
    }

    def init(text : String)
    {
      flush(Some(List(Text.Edit.insert(0, text))))
//      flush(None, Some(List(Text.Edit.insert(0, text))))
    }

    def +=(edit: Text.Edit)
    {
      pending += edit
    }
  }

  /* snapshot */
  def snapshot(): Document.Snapshot =
  {
    session.snapshot(thy_name, pending_edits.snapshot())
  }

  def init(text : String) {
    pending_edits.init(text)
  }

  def flush() {
    pending_edits.flush()
  }

  def insertText(offset : Int, text : String) {
    pending_edits += Text.Edit.insert(offset, text)
  }

  def removeText(offset : Int, text : String) {
    pending_edits += Text.Edit.remove(offset, text)
  }
  
  def getSession() = sessionFacade

}
