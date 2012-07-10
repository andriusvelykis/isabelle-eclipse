package isabelle.eclipse.core.resource

import isabelle.Document
import isabelle.Path
import isabelle.Thy_Header
import isabelle.Thy_Load
import isabelle.Library

/** An extension of theory loader that allows append directory path.
  * <p>
  * The {@link Path} structure currently does not carry the information whether it is a directory, 
  * e.g. /Root/foo can be either a file (with no extension) or a directory. By explicitly idenfying
  * whether it is a directory, we can resolve paths better.
  * </p> 
  * 
  * @author Andrius Velykis 
  */
class ThyLoad2 extends Thy_Load {

  /** Appends the (possibly) relative directory path to the base directory, thus resolving relative
    * paths if needed. 
    */
  def appendDir(dir: String, source_path: Path): String =
    append(dir, source_path)
    
  /** Redeclare name import as a workaround for explicitly stating when
    * directory paths are resolved for theory imports. 
    */
  private def import_name(dir: String, s: String): Document.Node.Name =
  {
    val theory = Thy_Header.base_name(s)
    if (is_loaded(theory)) Document.Node.Name(theory, "", theory)
    else {
      val path = Path.explode(s)
      val node = append(dir, thy_path(path))
      // use special method to resolve directory path
      val dir1 = appendDir(dir, path.dir)
      Document.Node.Name(node, dir1, theory)
    }
  }
  
  // redeclare header check to allow for the name import 
  override def check_header(name: Document.Node.Name, header: Thy_Header): Document.Node.Deps =
  {
    val name1 = header.name
    val imports = header.imports.map(import_name(name.dir, _))
    // FIXME val uses = header.uses.map(p => (append(name.dir, Path.explode(p._1)), p._2))
    val uses = header.uses
    if (name.theory != name1)
      Library.error("Bad file name " + thy_path(Path.basic(name.theory)) + " for theory " + Library.quote(name1))
    Document.Node.Deps(imports, header.keywords, uses)
  }
  
}
