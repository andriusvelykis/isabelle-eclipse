package isabelle.scala

import isabelle._

/**
 * A wrapper class for Document.Node.Name, since the Document.Node.Name cannot be referenced
 * in Java (problems with resolving such path - Object.Object.Class).
 */
object DocumentRef {
  
  def create(path: Path): DocumentRef = new DocumentRef(Document.Node.Name.apply(path))
  
  def create(node: String, dir: String, theory: String): DocumentRef = 
    new DocumentRef(Document.Node.Name(node, dir, theory))
  
  def create(command: Command) = new DocumentRef(command.node_name)
    
}

sealed class DocumentRef(ref: Document.Node.Name) {
 
  def getRef() = ref
  
  def getNode() = ref.node;
  
  def getDir() = ref.dir;
  
  def getTheory() = ref.theory;

  override def hashCode: Int = ref.hashCode

  override def equals(that: Any): Boolean =
    that match {
      case other: DocumentRef => ref == other.getRef
      case _ => false
    }
  
  override def toString: String = ref.toString()
  
}