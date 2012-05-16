package isabelle.eclipse.core.resource

import isabelle.Path
import isabelle.Thy_Load

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
  
}