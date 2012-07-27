package isabelle.eclipse.core.resource

import isabelle.Document
import isabelle.Isabelle_System
import isabelle.Library
import isabelle.Path
import isabelle.Thy_Load
import isabelle.Thy_Header
import isabelle.eclipse.core.IsabelleCorePlugin
import isabelle.eclipse.core.text.DocumentModel
import java.net.URI
import java.net.URISyntaxException
import org.eclipse.core.filesystem.EFS
import org.eclipse.core.filebuffers.FileBuffers
import org.eclipse.core.runtime.CoreException
import org.eclipse.emf.common.CommonPlugin
import org.eclipse.core.{runtime => erun}
import org.eclipse.core.{filesystem => efs}
import org.eclipse.emf.common.util.{URI => EmfURI}


/** A theory loader (and a companion object) that uses URIs to enable file-system operations. 
  * This goes in line with Eclipse's {@link EFS} functionality, which also employs URIs.
  * <p>
  * Every document reference is referenced with a URI, and relative theories are resolved against the
  * URIs. Absolute imported theories also have their paths converted to appropriate URIs. For
  * workspace files, the {@code platform:} URI scheme is used as provided by
  * {@link org.eclipse.emf.common.util.URI}.
  * </p>
  * <p>
  * As a workaround for a limitation in Isabelle with allowing URIs, they are encoded/decoded where
  * needed to "path" style to be allowed in Isabelle. See {@link URIPathEncoder} for details.
  * </p>
  * 
  * @author Andrius Velykis
  */
object URIThyLoad {
  
  /** Resolves parent URI for the given one. As proposed in
    * http://stackoverflow.com/questions/10159186/how-to-get-parent-url-in-java
    */
  def getParentURI(uri: URI): URI =
    if (uri.getPath.endsWith("/")) uri.resolve("..") else uri.resolve(".")


  /**
   * A URI-based view on Document.Node.Name.
   *
   * Each document is referenced by its URI and theory name.
   * Implicit conversion is provided between URINodeName and Document.Node.Name.
   */
  sealed case class URINodeName(uri: URI, theory: String)


  /** Creates a URI name. Assumes that the path is the String-encoded URI. */
  implicit def toURINodeName(name: Document.Node.Name): URINodeName = URINodeName(URI.create(name.node), name.theory)

  /** Creates a document name for the given URI. Parent URI (directory) is resolved from the node URI */
  implicit def toDocumentNodeName(name: URINodeName): Document.Node.Name = {
    val path = name.uri.toString
    val parentPath = getParentURI(name.uri).toString
    Document.Node.Name(path, parentPath, name.theory)
  }



  /** Appends (resolves) the source path against the given base URI */
  def resolveURI(base: URI, source_path: Path): URI = {
    // expand the path - this instantiates the path variables
    val path = source_path.expand

    if (path.is_absolute) {
      // path is absolute file system path - use Isabelle's libraries
      // to resolve (e.g. it has some cygwin considerations)
      val platformPath = Isabelle_System.platform_path(path);
      // encode as file system URI
      efs.URIUtil.toURI(platformPath)
    } else {
      // assume relative URI and resolve it against the base URI
      val pathStr = path.implode 

      try {
        val sourceUri = new URI(pathStr);
        base.resolve(sourceUri);
      } catch {
        case e: URISyntaxException => {
          IsabelleCorePlugin.log(e);
          // at the worst case, just append the path (expecting a directory here)
          erun.URIUtil.append(base, pathStr);
        }
      }
    }
  }


  def resolveDocumentUri(name: Document.Node.Name): URI = {
    // resolve platform URI if needed (gives filesystem URI for platform: (workspace) URIs)
    resolvePlatformUri(name.uri)
  }

  /** Resolves the given URI: if it was workspace-based ({@code platform:} scheme),
    * then it gets resolved to local filesystem.
    *
    * @param uriStr
    * @return
    */
  def resolvePlatformUri(uri: URI): URI = {
    // use EMF URI to resolve
    val emfUri = EmfURI.createURI(uri.toString);
    val resolvedUri = CommonPlugin.resolve(emfUri);
    URI.create(resolvedUri.toString());
  }

  /** Creates a workspace-based ({@code platform:} scheme) URI.
    *
    * @param path
    * @return
    */
  def createPlatformUri(path: String): URI = {
    // use EMF common libraries to construct "platform:" URI
    val uri = EmfURI.createPlatformResourceURI(path.toString(), true)
    URI.create(uri.toString())
  }
}

class URIThyLoad extends Thy_Load {
  
  import URIThyLoad._
  
  /** Use URI-based resolution of import names. */
  private def importName(baseUri: URI, s: String): Document.Node.Name =
  {
    val theory = Thy_Header.base_name(s)
    if (is_loaded(theory)) Document.Node.Name(theory, "", theory)
    else {
      // explode the path into parts
      val namePath = Path.explode(s)
      val path = thy_path(namePath)
      
      // resolve the relative path (similar to append, but can resolve relative paths)
      val nodeUri = resolveURI(baseUri, path)
      
      URINodeName(nodeUri, theory)
    }
  }
  
  /* Checks the theory header for consistency and resolves imported theories as document names.
   * 
   * Redeclared to allow for URI resolution.
   * 
   * @see isabelle.Thy_Load#check_header(Document.Node.Name, Thy_Header)
   */
  override def check_header(name: Document.Node.Name, header: Thy_Header): Document.Node.Deps =
  {
    val uri = name.uri
    val imports = header.imports.map(importName(uri, _))
    // FIXME val uses = header.uses.map(p => (append(name.dir, Path.explode(p._1)), p._2))
    val uses = header.uses
    if (name.theory != header.name)
      Library.error("Bad file name " + thy_path(Path.basic(name.theory)) + " for theory " + Library.quote(header.name))
    Document.Node.Deps(imports, header.keywords, uses)
  }

  /* Reads theory header for the given document reference. The implementation resolves the URI and
   * loads the file contents using Eclipse EFS, thus benefiting from the support for non-local
   * filesystems.
   * 
   * @see isabelle.Thy_Load#check_thy(Document.Node.Name)
   */
  override def read_header(name: Document.Node.Name): Thy_Header = {
    // resolve the document URI to load its contents
    val uri = URIThyLoad.resolveDocumentUri(name);

    try {
      val store = EFS.getStore(uri);

      // Load the file contents using FileBuffers. In this way if the file is already open by
      // some editor, we may avoid the need to reopen it, as the file buffer may be cached.
      val manager = FileBuffers.getTextFileBufferManager();
      manager.connectFileStore(store, null);
      val buffer = manager.getFileStoreTextFileBuffer(store);
      val fileText = buffer.getDocument().get();

      manager.disconnectFileStore(store, null);

      // read the header from the file
      Thy_Header.read(fileText);

    } catch {
      case e: CoreException => {
        IsabelleCorePlugin.log(e);
        // in case of failure, perform default loading
        super.read_header(name);
      }
    }
  }
  
}
