package isabelle.eclipse.core.resource;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.emf.common.CommonPlugin;

import isabelle.Isabelle_System;
import isabelle.Path;
import isabelle.Thy_Header;
import isabelle.eclipse.core.IsabelleCorePlugin;
import isabelle.scala.DocumentRef;
import isabelle.scala.RefThyLoad;

/**
 * A theory loader that uses URIs to enable file-system operations. This goes in line with Eclipse's
 * {@link EFS} functionality, which also employs URIs.
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
public class URIThyLoad extends RefThyLoad {

	/*
	 * Appends the (possibly) relative path to the base directory, thus resolving relative paths if
	 * needed.
	 * 
	 * (non-Javadoc)
	 * @see isabelle.Thy_Load#append(java.lang.String, isabelle.Path)
	 */
	@Override
	public String append(String dir, Path source_path) {
		
//		URI dirUri = URI.create(dir);
		URI dirUri = URIPathEncoder.decodePath(dir);
		URI resolvedUri = appendUri(dirUri, source_path);
		
//		return resolvedUri.toString();
		return URIPathEncoder.encodeAsPath(resolvedUri);
	}
	
	/**
	 * Appends (resolves) the source path against the given base directory URI.
	 * 
	 * @param dir
	 * @param source_path
	 * @return
	 */
	private URI appendUri(URI dir, Path source_path) {
		Path path = source_path.expand();
		
		if (path.is_absolute()) {
			// path is absolute file system path - use Isabelle's libraries
			// to resolve (e.g. it has some cygwin considerations)
			String platformPath = Isabelle_System.platform_path(path);
			// encode as file system URI
			return org.eclipse.core.filesystem.URIUtil.toURI(platformPath);
		} else {
			// assume relative URI and resolve it against the dir
			String pathStr = path.implode();
			
			try {
				URI sourceUri = new URI(pathStr);
				return dir.resolve(sourceUri);
			} catch (URISyntaxException e) {
				IsabelleCorePlugin.log(e);
				// at the worst case, just append the path to the directory
				return URIUtil.append(dir, pathStr);
			}
		}
	}

	/*
	 * Reads theory header for the given document reference. The implementation resolves the URI and
	 * loads the file contents using Eclipse EFS, thus benefiting from the support for non-local
	 * filesystems.
	 * 
	 * (non-Javadoc)
	 * 
	 * @see isabelle.Thy_Load#check_thy(DocumentRef)
	 */
	@Override
	public Thy_Header check_thy(DocumentRef name) {
		
		// resolve the document URI to load its contents
		URI uri = resolveDocumentUri(name);
		
		try {
			IFileStore store = EFS.getStore(uri);

			/*
			 * Load the file contents using FileBuffers. In this way if the file is already open by
			 * some editor, we may avoid the need to reopen it, as the file buffer may be cached.
			 */
			ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
			manager.connectFileStore(store, null);
			ITextFileBuffer buffer = manager.getFileStoreTextFileBuffer(store);
			String fileText = buffer.getDocument().get();
			
			manager.disconnectFileStore(store, null);
			
			return Thy_Header.read(fileText);
			
		} catch (CoreException e) {
			IsabelleCorePlugin.log(e);
			// in case of failure, perform default loading
			return super.check_thy(name);
		}
	}
	
	public static URI resolveDocumentUri(DocumentRef name) {
		
		String uriStr = name.getNode();
		URI platformUri = URIPathEncoder.decodePath(uriStr);
		
		// resolve platform URI if needed (gives filesystem URI for platform: (workspace) URIs)
		return resolvePlatformUri(platformUri.toString());
	}
	
	/**
	 * Resolves the given URI String: if it was workspace-based ({@code platform:} scheme), then it
	 * gets resolved to local filesystem.
	 * 
	 * @param uriStr
	 * @return
	 */
	public static URI resolvePlatformUri(String uriStr) {
		// use EMF URI to resolve
		org.eclipse.emf.common.util.URI uri = org.eclipse.emf.common.util.URI.createURI(uriStr);
		org.eclipse.emf.common.util.URI resolvedUri = CommonPlugin.resolve(uri);
		return URI.create(resolvedUri.toString());
	}
	
	/**
	 * Creates a workspace-based ({@code platform:} scheme) URI.
	 * 
	 * @param path
	 * @return
	 */
	public static URI createPlatformUri(String path) {
		// use EMF common libraries to construct "platform:" URI
		org.eclipse.emf.common.util.URI uri = org.eclipse.emf.common.util.URI
				.createPlatformResourceURI(path.toString(), true);
		return URI.create(uri.toString());
	}

}
