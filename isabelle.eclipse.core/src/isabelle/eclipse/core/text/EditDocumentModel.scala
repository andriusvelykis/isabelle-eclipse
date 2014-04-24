package isabelle.eclipse.core.text

import java.util.concurrent.locks.ReentrantReadWriteLock

import scala.collection.mutable.ListBuffer

import org.eclipse.core.runtime.{IProgressMonitor, NullProgressMonitor, Status}
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.jface.text.{DocumentEvent, IDocument, IDocumentListener}

import isabelle.{Document, Session, Text}
import isabelle.eclipse.core.util.{PostponeJob, SerialSchedulingRule}
import isabelle.eclipse.core.util.ConcurrentUtil.FunReadWriteLock


/**
 * A model for the Isabelle text document.
 *
 * It tracks changes in the text and submits them to the prover for evaluation when needed.
 *
 * @author Andrius Velykis
 */
object EditDocumentModel {

  /**
   * A rule to use in Job framework that ensures serial execution of jobs.
   * Used for submitting content to the Isabelle prover backend.
   */
  val serialSubmitRule = new SerialSchedulingRule
  
  // TODO add as a configurable option
  val flushDelay = 300
  
}

class EditDocumentModel(val session: Session,
                        val document: IDocument,
                        val name: Document.Node.Name) extends DocumentModel {

  private var activePerspectiveRange = Text.Range(0)
  private var submitOffset = 0
  
  private var pendingPerspective = false

  private def parseNodeHeader(): Document.Node.Header =
    session.thy_load.check_thy_text(name, document.get)

  /**
   * Indicate the document perspective: active portion of the document that should be processed
   * (and everything up to that portion)
   */
  override def setActivePerspective(offset: Int, length: Int) {
    val newRange = Text.Range(offset, offset + length)

    // make sure the new range is within the document
    val fixedRangeOpt = documentRange.try_restrict(newRange)

    // if the range has changed, mark it down and update the perspective
    fixedRangeOpt.filter(_ != activePerspectiveRange) foreach { range =>
      activePerspectiveRange = range
      updatePerspective()
    }
  }

  // TODO also allow explicitly saying how much to calculate - e.g. with a submitOffset?
  private def currentPerspective = Text.Perspective(List(activePerspectiveRange))

  /**
   * Indicate the offset up to which everything should be processed
   * TODO reactivate this support?
   */
  def setSubmitOffset(offset: Int) {

    val newOffset = documentRange.restrict(Text.Range(offset)).start
    if (newOffset != submitOffset) {
      submitOffset = newOffset
      updatePerspective()
    }
  }
  
  private def documentRange = Text.Range(0, math.max(document.getLength - 1, 0))

  private def updatePerspective() {
    pendingEdits.flushDelayed()
  }

  def submitFullPerspective(monitor: IProgressMonitor = new NullProgressMonitor) {

    // force flush current edits
    pendingEdits.doFlush(monitor)
    
    // submit the full perspective
    lockSubmit(monitor) {
      session.update(nodeEdits(Text.Perspective(List(documentRange)), Nil))
    }
  }

  /**
   * Checks if the contents of this document have already been submitted as-is to the prover
   */
  private def alreadySubmitted: Boolean = {
    val submitted = snapshot
    val submittedCmds = submitted.node.commands.toList
    val submittedText = (submittedCmds map (_.source)).mkString
    
    // check if the submitted text is the same as the current one - then submitted as-is
    submittedText == document.get
  }

  /* edits */

  private def initEdits(): List[Document.Edit_Text] = {

    val header = parseNodeHeader()
    val text = document.get
    val perspective = currentPerspective

    List(session.header_edit(name, header),
      name -> Document.Node.Clear(),
      name -> Document.Node.Edits(List(Text.Edit.insert(0, text))),
      name -> Document.Node.Perspective(true, perspective, Document.Node.Overlays.empty))
  }

  private def nodeEdits(perspective: Text.Perspective,
                        textEdits: List[Text.Edit]): List[Document.Edit_Text] = {

    val header = parseNodeHeader()

    List(session.header_edit(name, header),
      name -> Document.Node.Edits(textEdits),
      name -> Document.Node.Perspective(true, perspective, Document.Node.Overlays.empty))
  }
  
  
  /* pending text edits */

  private object pendingEdits {
    
    /** The pending edits (not yet submitted to the prover) */ 
    private val pending = new ListBuffer[Text.Edit]
    private var lastPerspective: Text.Perspective = Text.Perspective.empty

    // functional lock based on Java read/write lock
    private val lock = new ReentrantReadWriteLock()
    
    
    /** a job to perform edits in a separate (and delayed) thread */
    private val flushJob = new PostponeJob("Sending Changes to Prover", doFlush) {
      // set the submit rule
      override def config(job: Job) = job.setRule(EditDocumentModel.serialSubmitRule)
    }
    
    
    def snapshot(): List[Text.Edit] = lock.read{ pending.toList }

    /** Sends the pending edits to the prover without starting a separate job */
    def doFlush(monitor: IProgressMonitor = new NullProgressMonitor) = {
      val edits = lock.write {
        // copy edits for processing and clear the pending list
        val edits = snapshot()
        pending.clear

        edits
      }

      val newPerspective = currentPerspective

      if (!edits.isEmpty || lastPerspective != newPerspective) {
        lastPerspective = newPerspective

        lockSubmit(monitor) {
          session.update(nodeEdits(newPerspective, edits))
        }
      }

      Status.OK_STATUS
    }

    def flush(delay: Long = 0) = flushJob.run(delay)
    def flushDelayed() = flush(EditDocumentModel.flushDelay)

    def +=(edit: Text.Edit) {
      
      lock.write {
        pending += edit
      }

      flushDelayed()
    }

    def init() {
      doFlush()
      
      if (!alreadySubmitted) {
        // only reinitialise if not already submitted (avoid submitting everything again)

        // need a lock on the document for initialisation? E.g. to avoid edits while initialising?
        // technically this should come from the SWT thread so should not be any need?
        lockSubmit() {
          session.update(initEdits())
        }
      }
    }

    def exit() {
      // cancel possibly delayed job
      flushJob.cancel()
      // force flush
      doFlush()
    }
  }
  

  override def init() {
    // start listening on the document
    document.addDocumentListener(documentListener)
    pendingEdits.init()
  }

  override def dispose() {
    document.removeDocumentListener(documentListener);
    pendingEdits.exit()
  }

  override def snapshot(): Document.Snapshot = session.snapshot(name, pendingEdits.snapshot())
    
  /** Listener for document changes - updates the edit queue */
  private val documentListener = new IDocumentListener {

    override def documentChanged(event: DocumentEvent) {
      // do the inserts after the change
      if (!event.getText().isEmpty()) {

        if (event.getOffset() <= submitOffset) {
          // update the offset with inserted
          submitOffset = submitOffset + event.getText.length
        }

        // something's inserted
        pendingEdits += Text.Edit.insert(event.getOffset, event.getText)
      }
    }

    override def documentAboutToBeChanged(event: DocumentEvent) {
      // do the removals before the change
      if (event.getLength > 0) {
        
        // Replaced text is non empty - something was removed.
        // Note, if a replacement is going on, it will be caught in #documentChanged(),
        // and an additional insert will be added. So a replacement will be represented
        // as Remove-Insert in Isabelle.
        val removedText = event.getDocument.get(event.getOffset, event.getLength)

        if (event.getOffset <= submitOffset) {
          // update the offset with removed
          submitOffset = submitOffset - event.getLength
        }

        pendingEdits += Text.Edit.remove(event.getOffset(), removedText)
      }
    }
  };

  /** Locks the submit to Isabelle. This is used to wrap the submit to Isabelle
    * into a scheduling rule, enforcing sequential submits. Since the rule can
    * be nested, it will be ok if called from a flush job already.
    */
  private def lockSubmit(monitor: IProgressMonitor = new NullProgressMonitor)(f: => Unit) {

    val jobs = Job.getJobManager

    val submitRule = EditDocumentModel.serialSubmitRule
    jobs.beginRule(submitRule, monitor)
    f
    jobs.endRule(submitRule)
  }
  
}
