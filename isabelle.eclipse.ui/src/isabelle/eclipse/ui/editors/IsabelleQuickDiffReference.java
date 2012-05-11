package isabelle.eclipse.ui.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.quickdiff.IQuickDiffReferenceProvider;

// TODO implement, see LastSaveReferenceProvider
public class IsabelleQuickDiffReference implements IQuickDiffReferenceProvider {

	@Override
	public IDocument getReference(IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setActiveEditor(ITextEditor editor) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setId(String id) {
		// TODO Auto-generated method stub
		
	}

}
