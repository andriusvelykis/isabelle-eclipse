package isabelle.eclipse.core.text

import org.eclipse.jface.text.IDocument

import isabelle.{Document, Session}


/**
 * A read-only version of Isabelle text document model: only queries the snapshot but
 * does not send any edit changes to the prover.
 *
 * @author Andrius Velykis
 */
class ReadOnlyDocumentModel(val session: Session,
                            val document: IDocument,
                            val name: Document.Node.Name) extends DocumentModel {

  override def snapshot(): Document.Snapshot = session.snapshot(name)

  override def init() {}

  override def dispose() {}

  override def setActivePerspective(offset: Int, length: Int) {}

}
