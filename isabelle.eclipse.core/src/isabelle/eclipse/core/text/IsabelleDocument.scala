package isabelle.eclipse.core.text

import isabelle.Symbol
import isabelle.eclipse.core.IsabelleCorePlugin
import org.eclipse.jface.text.BadLocationException
import org.eclipse.jface.text.Document
import org.eclipse.jface.text.DocumentEvent
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.IDocumentExtension4
import org.eclipse.jface.text.IDocumentListener
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.Region
import scala.collection.mutable.WeakHashMap


/** A document containing text with Isabelle Unicode symbols (as opposed to the ASCII version
  * that gets saved to disk).
  * 
  * The document is based on the ASCII version of Isabelle theory file (the `base` parameter).
  * It keeps contents of both documents in sync and propagates changes between the documents,
  * while transcoding between Isabelle Unicode and ASCII representations.
  * 
  * Such sync is performed because we cannot define the conversion as new Java Encoding (similar
  * to 'UTF8' or 'ISO 8859-1') with Eclipse's OSGI class loaders. To define new encoding, we would
  * need to add it to root class loader, and thus edit Eclipse's launch configuration. In plug-in
  * based solution, this is not a good approach.
  * 
  * Furthermore by keeping the 'base' document in sync, we can support Eclipse's multiple editors,
  * e.g. edit Isabelle document both in editor that supports the Unicode symbols and a basic text
  * editor to show ASCII version.
  * 
  * The synchronisation is performed on every change on any of the documents. We assume that
  * encoding only affects words/symbols and the newlines match between both documents. Therefore
  * during a document change we only need to analyse and transcode the affected lines. Then
  * we use `diff` to identify the actual change as minimally as possible (see `syncEvent()`).  
  * 
  * @author Andrius Velykis
  */
class IsabelleDocument(val base: IDocument) extends Document {

  // import methods from the companion object to avoid full name referencing
  import IsabelleDocument._

  // keep sync listeners after initialisation to disconnect when no longer used
  private val (baseListener, thisListener) = {
    
    // wrap the documents to allow flagging them during the sync
    // this is needed to avoid sync loop
    val baseDoc = new UpdatingDocument(base)
    val thisDoc = new UpdatingDocument(this);

    // init listeners to keep the documents in sync
    val baseListener = keepInSync(baseDoc, thisDoc, Symbol.decode)
    val thisListener = keepInSync(thisDoc, baseDoc, Symbol.encode)

    // do initial sync from base to this (current document is empty)
    syncAll(baseDoc, thisDoc, Symbol.decode)

    (baseListener, thisListener)
  }
  
  def dispose() {
    // disconnect the document listeners, because Base document can be reused
    base.removeDocumentListener(baseListener)
    this.removeDocumentListener(thisListener)
  }
  
}

object IsabelleDocument {
  
  /** Wrap a document with a flag that it is being updated by us. */
  private class UpdatingDocument(val document: IDocument) { var updating = false }

  /** Attaches a listener on the `from` document to transcode and sync the changes to the `to` document. */
  private def keepInSync(from: UpdatingDocument, to: UpdatingDocument, transcode: String => String): IDocumentListener = {
    // attach a listener to 'from' document and sync to 'to' document when it changes
    val syncListener = lineListener { (event, originalEndLine) =>
      {
        // check if document is being updated by us, then do not react to changes
        if (!from.updating) {
          // synchronise the event changes
          syncEvent(from, to, transcode, event, originalEndLine)
        }
      }
    }
    
    from.document.addDocumentListener(syncListener)
    syncListener
  }

  /** Creates a document listener that captures the end line of the document change.
    * This is necessary, because the change could have spanned several lines and after
    * the change we can no longer determine the replaced line information.
    */
  private def lineListener(f: (DocumentEvent, Option[Int]) => Unit) =
    new IDocumentListener {

      // stores the end line for the document event
      val eventEndLines = WeakHashMap[DocumentEvent, Int]()

      override def documentChanged(event: DocumentEvent) = f(event, eventEndLines.remove(event))
      override def documentAboutToBeChanged(event: DocumentEvent) {
        
        // get the end line in the original document (in the new document the lines may get replaced)
        val endLine = badLoc{ event.getDocument.getLineOfOffset(event.getOffset + event.getLength) }
        endLine foreach (eventEndLines.put(event, _))
      }
    }
  
  /** Transcodes all the text from the `from` document to the `to` document.
    * No diff is performed because all text is replaced. 
    */
  private def syncAll(from: UpdatingDocument, to: UpdatingDocument, transcode: String => String) = {

    val text = from.document.get()

    val isabelle = IsabelleCorePlugin.getIsabelle
    val transcoded = if (isabelle.isInit) {
      transcode(text)
    } else {
      text
    }

    to.updating = true
    (to.document, from.document) match {
      // for modern documents, set the same modification stamp as the original
      case (to: IDocumentExtension4, from: IDocumentExtension4) => to.set(transcoded, from.getModificationStamp);
      // just set the text
      case (to, _) => to.set(transcoded)
    }
    to.updating = false
  }
  
  /** Transcodes the affected text as indicated by the document event. Isolates the affected lines
    * and performs `diff` to capture word-level (sometimes even symbol-level) changes.
    */
  private def syncEvent(from: UpdatingDocument, to: UpdatingDocument, transcode: String => String,
      event: DocumentEvent, originalFromEndLine: Option[Int]) {

    val isabelle = IsabelleCorePlugin.getIsabelle
    // check if Isabelle is initialised, otherwise we do not have access to symbol encodings
    val edit = if (isabelle.isInit) {
      
      // Find the regions in both documents affected by the document change event.
      // We assume that symbol encodings do not span multiple lines. Therefore we find the lines
      // affected by the document event and retrieve all the text in those lines.
      // This way we minimise the text we need to diff, which is a bit more expensive operation.
      val (fromRegion, toRegion) = affectedRegions(from.document, to.document, event, originalFromEndLine)
      
      // get the affected texts
      def text(doc: IDocument, region: IRegion) = doc.get(region.getOffset, region.getLength)
      val fromText = text(from.document, fromRegion)
      val toText = text(to.document, toRegion)
      
      // transcode the text in original document
      val transcoded = transcode(fromText)
      
      // compare the transcoded text with what is already in the target document - find the differences
      val diff = minContiguousDiff(toText, transcoded)
      
      // adapt the differences to the absolute offset (they are 0-based from the `toText`)
      diff map {
        case (offset, length, replaceText) => (toRegion.getOffset + offset, length, replaceText)
      }
    } else {
      // Isabelle not initialised, so no transcoding is done - just forward the original text replacement
      Some((event.getOffset, event.getLength, event.getText))
    }
    
    to.updating = true
    // apply the edit
    edit foreach { case (offset, length, text) =>
      to.document match {
        // if modern documents are used, also set the modification stamp
        case d: IDocumentExtension4 => d.replace(offset, length, text, event.getModificationStamp)
        case d => d.replace(offset, length, text)
      }
    }
    to.updating = false
  }

  private def affectedRegions(from: IDocument, to: IDocument,
      event: DocumentEvent, originalFromEndLine: Option[Int]): (IRegion, IRegion) = {

    // get the start/end lines for the 'from' document (after the event changes)
    val fromStartLine = badLoc { from.getLineOfOffset(event.getOffset) }
    val fromEndLine = badLoc { from.getLineOfOffset(event.getOffset + event.getText.length) }

    // get the offset of start line in both 'from' and 'to' documents
    val fromStartOpt = fromStartLine.flatMap(lineStart(from, _))
    val toStartOpt = fromStartLine.flatMap(lineStart(to, _))

    // get the end offset of the end line in the 'from' document
    val fromEndOpt = fromEndLine.flatMap(lineEnd(from, _))
    // to determine end of 'to' document, use the original end line, since it may have changed in the 'from' document
    val toEndOpt = originalFromEndLine.flatMap(lineEnd(to, _))

    // if cannot determine the offset of the start line, just use the start of the whole document
    val (fromStart, toStart) = getBothOrElse((fromStartOpt, toStartOpt), (0, 0))
    // if there were problems finding the ends of the affected lines, just use the ends of the whole documents
    val (fromEnd, toEnd) = getBothOrElse((fromEndOpt, toEndOpt), (from.getLength, to.getLength))

    // return affected regions (full lines) in both 'from' and 'to' documents
    def region(start: Int, end: Int) = new Region(start, end - start)
    (region(fromStart, fromEnd), region(toStart, toEnd))
  }
  
  private def getBothOrElse[A, B](opt: (Option[A], Option[B]), elseVal: => (A, B)): (A, B) =
    opt match {
      case (Some(v1), Some(v2)) => (v1, v2)
      case _ => elseVal
    }
  
  private def lineStart(document: IDocument, line: Int) =
    badLoc { document.getLineOffset(line) }
  private def lineEnd(document: IDocument, line: Int) =
    badLoc { document.getLineOffset(line) + document.getLineLength(line) }

  /** A wrapper for code that can throw BadLocationException. If one is caught, None is returned to
    * indicate that the value is invalid.
    */
  private def badLoc[A](f: => A): Option[A] = {
    try {
      Some(f)
    } catch {
      case e: BadLocationException => None
    }
  }
  
  /** Finds the minimal contiguous diff between the old text and the new text. Returns the diff
    * as 'replacement' edit to use in document.
    * 
    * We are using a contiguous diff instead of multiple small replacements to avoid multiple issues.
    * For example, we need to keep the modification stamp for correct 'undo' functionality -
    * otherwise we would need to spoof fake modification stamps (see previous versions of the document).
    * Furthermore, with multiple replacements we need to adjust offsets of subsequent replacements
    * to take into account earlier replacements (e.g. if new text was added, the other offsets have changed).
    */
  private def minContiguousDiff(oldText: String, newText: String): Option[(Int, Int, String)] = {
      // compare the transcoded text with what is already in the target document - find the differences
      val diffs = DiffUtils.diff(oldText, newText)
      
      diffs match {
        case Nil => None
        // single element
        case ((offset, oldText), (_, newText)) :: Nil => Some(offset, oldText.length, newText)
        case multiple => {
          // for multiple diffs, make the minimum contiguous range
          // so take the first and last diffs and construct the range between them
          val ((firstOldOffset, _), (firstNewOffset, _)) = multiple.head
          val (lastOld, lastNew) = multiple.last
          
          // get the end points
          def endOffset: ((Int, String)) => Int = { case (offset, text) => offset + text.length }
          val (endOld, endNew) = (endOffset(lastOld), endOffset(lastNew))
          
          // offset, length in old + new text range
          Some(firstOldOffset, endOld - firstOldOffset, newText.substring(firstNewOffset, endNew))
        }
      }
  }
  
}
