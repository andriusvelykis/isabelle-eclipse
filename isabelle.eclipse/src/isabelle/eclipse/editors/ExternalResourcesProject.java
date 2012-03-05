package isabelle.eclipse.editors;

import isabelle.eclipse.IsabelleEclipsePlugin;

import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;

/**
 * Copied from org.eclipse.compare.internal.CompareWithOtherResourceDialog
 * 
 * @author andrius
 */
class ExternalResourcesProject {
	
	// Implementation based on org.eclipse.jdt.internal.core.ExternalFoldersManager
	
	private int counter = 0;
	
	private static final String TMP_PROJECT_NAME = ".isabelle.eclipse.external"; //$NON-NLS-1$
	
	private final static String TMP_PROJECT_FILE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //$NON-NLS-1$
			+ "<projectDescription>\n" //$NON-NLS-1$
			+ "\t<name>" + TMP_PROJECT_NAME + "\t</name>\n" //$NON-NLS-1$ //$NON-NLS-2$
			+ "\t<comment></comment>\n" //$NON-NLS-1$
			+ "\t<projects>\n" //$NON-NLS-1$
			+ "\t</projects>\n" //$NON-NLS-1$
			+ "\t<buildSpec>\n" //$NON-NLS-1$
			+ "\t</buildSpec>\n" //$NON-NLS-1$
			+ "\t<natures>\n" + "\t</natures>\n" //$NON-NLS-1$//$NON-NLS-2$
			+ "</projectDescription>"; //$NON-NLS-1$
	
	private final static String TMP_FOLDER_NAME = "linkedFiles"; //$NON-NLS-1$
	
	public ExternalResourcesProject() {
		// nothing to do here
	}
	
	private IProject createTmpProject() throws CoreException {
		IProject project = getTmpProject();
		if (!project.isAccessible()) {
			try {
				IPath stateLocation = IsabelleEclipsePlugin.getDefault().getStateLocation();
				if (!project.exists()) {
					IProjectDescription desc = project.getWorkspace().newProjectDescription(project.getName());
					desc.setLocation(stateLocation.append(TMP_PROJECT_NAME));
					project.create(desc, null);
				}
				try {
					project.open(null);
				} catch (CoreException e) { // in case .project file or folder has been deleted
					IPath projectPath = stateLocation.append(TMP_PROJECT_NAME);
					projectPath.toFile().mkdirs();
					FileOutputStream output = new FileOutputStream(
							projectPath.append(".project").toOSString()); //$NON-NLS-1$
					try {
						output.write(TMP_PROJECT_FILE.getBytes());
					} finally {
						output.close();
					}
					project.open(null);
				}
				getTmpFolder(project);
			} catch (IOException ioe) {
				return project;
			} catch (CoreException ce) {
				throw new CoreException(ce.getStatus());
			}
		}
		project.setHidden(true);
		return project;
	}
	
	private IFolder getTmpFolder(IProject project) throws CoreException {
		IFolder folder = project.getFolder(TMP_FOLDER_NAME);
		if (!folder.exists())
			folder.create(IResource.NONE, true, null);
		return folder;
	}
	
	public IFile getExternalFile(IPath path) {
		if (path != null)
			return (IFile) linkResource(path);
		return null;
	}

	public IFolder getExternalFolder(IPath path) {
		if (path != null)
			return (IFolder) linkResource(path);
		return null;
	}
	
	private IResource linkResource(IPath path) {
		IResource r = null;
		String resourceName = path.lastSegment();
		try {
			IProject project = createTmpProject();
			if (!project.isOpen())
				project.open(null);
			if (path.toFile().isFile()) {
				r = getTmpFolder(project).getFile(resourceName);
				if (r.exists()) { 	// add a number to file's name when there already is a file with that name in a folder
					String extension = path.getFileExtension();
					String fileName = path.removeFileExtension().lastSegment(); 
					r = getTmpFolder(project).getFile(getName(fileName, extension));
				}
				((IFile)r).createLink(path, IResource.REPLACE, null);
			} else { // isDirectory
				r = getTmpFolder(project).getFolder(resourceName);
				if (r.exists()) {
					r = getTmpFolder(project).getFolder(getName(resourceName, null));
				}
				((IFolder)r).createLink(path, IResource.REPLACE, null);
			}
		} catch (CoreException e) {
			IsabelleEclipsePlugin.log(e.getLocalizedMessage(), e);
			MessageDialog.openError(PlatformUI.getWorkbench().getDisplay().getActiveShell(),
							"Error",
							e.getLocalizedMessage());
		}
		return r;
	}
	
	/**
	 * This method is used to prevent duplicating names of linked resources.
	 * It adds a suffix based on the <code>counter</code> value.
	 * 
	 * @param name
	 * @param extension optional
	 * @return
	 */
	private String getName(String name, String extension) {
		if (counter != 0) {
			name = name + "-" + counter; //$NON-NLS-1$
		}
		// at most 3 resources at the same time with the same name:
		// left, right, ancestor
		counter = (counter + 1) % 3;
		if (extension != null) {
			name += "." + extension; //$NON-NLS-1$
		}
		// don't change the name if counter equals 0
		return name;
	}
	
	private IProject getTmpProject() {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(
				TMP_PROJECT_NAME);
	}
}
