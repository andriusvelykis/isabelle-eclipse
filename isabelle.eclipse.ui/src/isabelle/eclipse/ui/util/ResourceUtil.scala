package isabelle.eclipse.ui.util

import java.net.URI
import java.net.URISyntaxException

import org.eclipse.core.filesystem.URIUtil
import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.ui.IEditorInput
import org.eclipse.ui.editors.text.ILocationProvider
import org.eclipse.ui.editors.text.ILocationProviderExtension
import org.eclipse.ui.ide

import isabelle.eclipse.core.resource.URIThyLoad
import isabelle.eclipse.core.util.AdapterUtil.adapt

/** Resource management utility methods.
  * 
  * @author Andrius Velykis
  */
object ResourceUtil {

  /** Resolves a URI for the given editor input. For workspace files, the `platform:` URI scheme
    * is used, for the rest it is resolved through IFileStore. Local filesystem files have `file:` URIs.
    *
    * @param input
    * @return the URI corresponding to the given editor input, or `None` if one cannot be resolved.
    * @throws URISyntaxException
    */
  @throws(classOf[URISyntaxException])
  def getInputURI(input: IEditorInput): Option[URI] = {

    // first check if the input is on a workspace file
    val file = Option(ide.ResourceUtil.getFile(input))

    // encode the path as `platform:` URI
    file.map(getResourceURI) orElse {

      // otherwise try to resolve the URI or IPath
      val locProvider = adapt(input)(classOf[ILocationProvider])

      locProvider flatMap { provider =>
        {
          // get the URI, if available
          val extUri = provider match {
            case providerExt: ILocationProviderExtension => Option(providerExt.getURI(input))
            case _ => None
          }

          // use the found URI
          extUri orElse {

            // otherwise go via the path
            val path = Option(provider.getPath(input))

            // the path can be either workspace, or file system
            // check if it is in the workspace first
            path.map(p => {

              val res = Option(ResourcesPlugin.getWorkspace.getRoot.findMember(p))

              // if found in workspace, use workspace path
              // otherwise, treat as absolute file system path
              res.map(getResourceURI).getOrElse(URIUtil.toURI(p))
            })
            
          }
        }
      }
    }
  }

  /** Creates a workspace resource URI using `platform:` scheme.
    *
    * @throws URISyntaxException
    */
  @throws(classOf[URISyntaxException])
  private def getResourceURI(resource: IResource): URI = {
    // use `file:` URIs, because Isabelle2012 does not allow `platform:` URI scheme
    // TODO review with Isabelle2013
    resource.getLocationURI()
//    val path = resource.getFullPath()
//    URIThyLoad.createPlatformUri(path.toString)
  }

}
