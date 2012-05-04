package isabelle.eclipse.core.resource;

import isabelle.eclipse.core.IsabelleCorePlugin;

import java.net.URI;

/**
 * A class to provide workaround for Isabelle's lack of support for URI-based paths. Isabelle does
 * not allow colon ":" character in paths, thus making it impossible to use URIs such as
 * {@code file:/dir/file.thy} to indicate theory locations. As a workaround, I am replacing the ":"
 * character with an allowed one "`", which Isabelle is happy with.
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
	public static URI decodePath(String path) {
		
		int sep = path.indexOf(SCHEME_PATH_SEP);
		if (sep < 0) {
			// should have found at least something..
			IsabelleCorePlugin.log("Decoding unencoded path: " + path, null);
			return URI.create(path);
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
