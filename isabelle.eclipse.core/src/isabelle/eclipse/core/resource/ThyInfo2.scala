package isabelle.eclipse.core.resource

import isabelle.Document
import isabelle.Path
import isabelle.Thy_Header
import isabelle.Thy_Info
import isabelle.Thy_Load

/** An extension of theory information as a workaround for explicitly stating when
  * directory paths are resolved for theory imports. 
  * 
  * @author Andrius Velykis 
  */
class ThyInfo2(thy_load: ThyLoad2) extends Thy_Info(thy_load) {

  override def import_name(dir: String, str: String): Document.Node.Name =
  {
    val path = Path.explode(str)
    val node = thy_load.append(dir, Thy_Header.thy_path(path))
    // use special method to resolve directory path
    val dir1 = thy_load.appendDir(dir, path.dir)
    val theory = path.base.implode
    Document.Node.Name(node, dir1, theory)
  }
  
}