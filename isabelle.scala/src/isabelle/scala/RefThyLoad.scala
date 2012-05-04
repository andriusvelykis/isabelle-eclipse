package isabelle.scala

import isabelle._

/**
 * An extension of Thy_Load to avoid reference of Document.Node.Name when 
 * overriding #check_thy(). Providing the same method with DocumentRef for
 * overriding instead.
 * 
 * @author Andrius Velykis
 * @see DocumentRef
 */
class RefThyLoad extends Thy_Load {
  
  override def check_thy(name: Document.Node.Name): Thy_Header =
    check_thy(new DocumentRef(name))
    
  protected def check_thy(name: DocumentRef): Thy_Header =
    super.check_thy(name.getRef());
    
    
}