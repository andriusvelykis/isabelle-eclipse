package isabelle.eclipse.core.resource

import java.net.{URI, URISyntaxException}

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

import org.eclipse.core.filebuffers.FileBuffers
import org.eclipse.core.{filesystem => efs}
import org.eclipse.core.filesystem.EFS
import org.eclipse.core.{runtime => erun}
import org.eclipse.core.runtime.CoreException
import org.eclipse.emf.common.CommonPlugin
import org.eclipse.emf.common.util.{URI => EmfURI}

import isabelle.{Document, Isabelle_System, Outer_Syntax, Path, Symbol, Thy_Header, Thy_Load}
import isabelle.eclipse.core.internal.IsabelleCorePlugin.{error, log}


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
      isabellePathUri(path)
    } else {
      // assume relative URI and resolve it against the base URI
      val pathStr = path.implode 

      try {
        val sourceUri = new URI(pathStr);
        base.resolve(sourceUri);
      } catch {
        case e: URISyntaxException => {
          log(error(Some(e)));
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

  /**
   * Retrieves URI of the Isabelle Path representing a file system path
   */
  def isabellePathUri(path: Path): URI = {
    // path is absolute file system path - use Isabelle's libraries
    // to resolve (e.g. it has some cygwin considerations)
    val platformPath = Isabelle_System.platform_path(path)
    // encode as file system URI
    efs.URIUtil.toURI(platformPath)
  }
  
}

class URIThyLoad(loaded_theories: Set[String] = Set.empty, base_syntax: Outer_Syntax)
    extends Thy_Load(loaded_theories, base_syntax) {
  
  import URIThyLoad._
  
  /*
   * Resolves imported document names.
   * 
   * Overloaded to allow for URI resolution.
   */
  override def import_name(base_name: Document.Node.Name, s: String): Document.Node.Name =
  {
    val baseUri = base_name.uri
    val theory = Thy_Header.base_name(s)
    if (loaded_theories(theory)) Document.Node.Name(theory, "", theory)
    else {
      // explode the path into parts
      val namePath = Path.explode(s)
      val path = Thy_Load.thy_path(namePath)
      
      // resolve the relative path (similar to append, but can resolve relative paths)
      val nodeUri = resolveURI(baseUri, path)
      URINodeName(nodeUri, theory)
    }
  }
  
  
  @throws[CoreException]
  private def loadDocumentContents(name: Document.Node.Name): String = {
    // resolve the document URI to load its contents
    val uri = URIThyLoad.resolveDocumentUri(name)
    
    val store = EFS.getStore(uri)

    // Load the file contents using FileBuffers. In this way if the file is already open by
    // some editor, we may avoid the need to reopen it, as the file buffer may be cached.
    val manager = FileBuffers.getTextFileBufferManager
    manager.connectFileStore(store, null)
    val buffer = manager.getFileStoreTextFileBuffer(store)
    val fileText = buffer.getDocument.get

    manager.disconnectFileStore(store, null)
    
    fileText
  }

  /*
   * Performs document operations that require its text.
   * 
   * The implementation resolves the URI and loads the file contents using Eclipse EFS,
   * thus benefiting from the support for non-local filesystems.
   */
  override def with_thy_text[A](name: Document.Node.Name, f: CharSequence => A): A = {
    
    val documentText = Try(loadDocumentContents(name))

    documentText match {
      
      case Success(text) => {
        Symbol.decode_strict(text)
        f(text)
      }
      
      case Failure(e) => {
        log(error(Some(e)))
        // in case of failure, perform default loading
        super.with_thy_text(name, f)
      }
    }
  }
  
}
