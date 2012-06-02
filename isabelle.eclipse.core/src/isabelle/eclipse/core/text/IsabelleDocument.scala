package isabelle.eclipse.core.text

import isabelle.Symbol
import isabelle.eclipse.core.IsabelleCorePlugin
import org.eclipse.jface.text.Document
import org.eclipse.jface.text.DocumentEvent
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.IDocumentListener


class IsabelleDocument(val base: IDocument) extends Document {

  import IsabelleDocument._

  private val (baseListener, thisListener) = {
    val baseDoc = new FlagDocument(base)
    val thisDoc = new FlagDocument(this);

    // init listeners to keep the documents in sync
    val baseListener = keepInSync(baseDoc, thisDoc, Symbol.decode)
    val thisListener = keepInSync(thisDoc, baseDoc, Symbol.encode)

    // do initial sync from base to this
    sync(baseDoc, thisDoc, Symbol.decode)

    (baseListener, thisListener)
  }
  
  def dispose() {
    // disconnect the document listeners, because Base document can be reused
    base.removeDocumentListener(baseListener)
    this.removeDocumentListener(thisListener)
  }
  
}

object IsabelleDocument {
  
  /** Encapsulate a document with a flag that it is being updated by us. */
  private class FlagDocument(val document: IDocument) { var updating = false }

  private def keepInSync(from: FlagDocument, to: FlagDocument, transcode: String => String): IDocumentListener = {
    // attach a listener to 'from' document and sync to 'to' document when it changes
    val syncListener = listener { _ =>
      {
        // check if document is being updated by us, then do not react to changes
        if (!from.updating) {
          sync(from, to, transcode)
        }
      }
    }
    
    from.document.addDocumentListener(syncListener)
    syncListener
  }
  
  private def listener(f: DocumentEvent => Unit) =
    new IDocumentListener {
      override def documentChanged(event: DocumentEvent) = f(event)
      override def documentAboutToBeChanged(event: DocumentEvent) {}
    }
  
  private def sync(from: FlagDocument, to: FlagDocument, transcode: String => String) = {

    val text = from.document.get()

    val isabelle = IsabelleCorePlugin.getIsabelle
    val transcoded = if (isabelle.isInit) {
      transcode(text)
    } else {
      text
    }

    to.updating = true
    to.document.set(transcoded)
    to.updating = false
  }
  
}
