package isabelle.eclipse.core.text

import org.eclipse.jface.text.IDocument

import isabelle.{Document, Session}


/**
 * A trait for models of the Isabelle text document.
 *
 * @author Andrius Velykis
 */
trait DocumentModel {

  def session: Session

  def document: IDocument

  def name: Document.Node.Name

  def snapshot: Document.Snapshot

  def init()

  def dispose()

  def setActivePerspective(offset: Int, length: Int)
}
