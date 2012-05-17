package isabelle.eclipse.core.resource;

import java.io.File;
import java.net.URI;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.URIUtil;

/**
 * A class to provide workaround for Isabelle's lack of support for URI-based paths. Isabelle does
 * not allow colon ":" character in paths, thus making it impossible to use URIs such as
 * {@code file:/dir/file.thy} to indicate theory locations. As a workaround, I am replacing the ":"
 * character with an allowed one "`", which Isabelle is happy with.
 * <p>
 * To support path resolution in ML side, the local filesystem URIs are encoded as file paths. The URI
 * encoding is still kept for non-local filesystem URIs.
 * </p>
 * 
 * @author Andrius Velykis
 */
public class URIPathEncoder {

	private static final String SCHEME_SEP = ":";
	private static final String SCHEME_PATH_SEP = "`";
	
	/**
	 * Encodes a URI as path: replaces scheme separator ":" with a different (allowed) separator.
	 * <p>
	 * For example, URI {@code file:/dir/file.thy} becomes {@code file`/dir/file.thy}, which
	 * Isabelle accepts.
	 * </p>
	 * 
	 * @param uri
	 * @return
	 */
	public static String encodeAsPath(URI uri) {
		
		String uriStr = uri.toString();
		
		URI fileUri = URIThyLoad.resolvePlatformUri(uriStr);
		if (URIUtil.isFileURI(fileUri)) {
			// file URI - use it as the path instead of encoding the URI.
			// this is needed because on Isabelle side, ML Path expects file paths
			// and if we use encoded URIs, ML code cannot resolve relative paths
			File file = URIUtil.toFile(fileUri);
			return file.getPath();
		}
		
		String pathStr;
		if (uri.getScheme() == null) {
			// no URI scheme is set - add a separator symbol at the beginning
			pathStr = SCHEME_PATH_SEP + uriStr;
		} else {
			// replace the first colon with the scheme sep
			pathStr = uriStr.replaceFirst(SCHEME_SEP, SCHEME_PATH_SEP);
		}
		
		return pathStr;
	}
	
	/**
	 * Decodes an encoded path back to URI: replaces the custom separator back to the scheme
	 * separator ":".
	 * 
	 * @param path
	 * @return
	 */
	public static URI decodePath(String path, boolean isDir) {
		
		int sep = path.indexOf(SCHEME_PATH_SEP);
		int nameSep = path.indexOf("/");
		if (sep < 0 || sep >= nameSep) {
			// decoding a file path instead of the encoded URI
			URI fileUri = org.eclipse.core.filesystem.URIUtil.toURI(path);
			
			// check if the file is in the workspace - then use workspace URI
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IResource[] workspaceResources = isDir 
					? root.findContainersForLocationURI(fileUri)
					: root.findFilesForLocationURI(fileUri);
					
			URI wsUri;
			if (workspaceResources.length > 0) {
				// found a workspace resource - take first and decode as "platform:" URI
				IResource res = workspaceResources[0];
				IPath workspacePath = res.getFullPath();
				wsUri = URIThyLoad.createPlatformUri(workspacePath.toString());
			} else {
				// outside the workspace, use the file URI
				wsUri = fileUri;
			}
			
			// ensure the directory if needed
			// paths lose the trailing slash, which is needed to resolve relative URIs
			// for directories (e.g. file:/Root/foo is a file, file:/Root/foo/ is a dir)
			return isDir ? URI.create(wsUri.toString() + "/") : wsUri;
		}
		
		if (sep == 0) {
			// no URI scheme, use the path as is
			return URI.create(path);
		} else {
			
			// replace first encoded separator with a colon
			String uriStr = path.replaceFirst(SCHEME_PATH_SEP, SCHEME_SEP);
			return URI.create(uriStr);
		}
	}
	
}
