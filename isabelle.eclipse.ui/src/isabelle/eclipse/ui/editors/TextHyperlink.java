package isabelle.eclipse.ui.editors;

import isabelle.eclipse.ui.IsabelleUIPlugin;

import java.net.URI;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;


/**
 * A hyperlink to a text editor. Opens the given URI and selects target region in the opened editor.
 * 
 * @author Andrius Velykis
 */
public class TextHyperlink implements IHyperlink {

	private final IWorkbenchPage page;
	private final IRegion linkRegion;
	private final URI targetUri;
	private final String targetName;
	private final IRegion highlightRegion;
	private final IRegion selectRegion;
	
	public TextHyperlink(IWorkbenchPage page, IRegion linkRegion, URI targetUri, String targetName,
			IRegion selectRegion, IRegion highlightRegion) {
		
		this.page = page;
		this.linkRegion = linkRegion;
		this.targetUri = targetUri;
		this.targetName = targetName;
		this.highlightRegion = highlightRegion;
		this.selectRegion = selectRegion;
	}

	@Override
	public IRegion getHyperlinkRegion() {
		return linkRegion;
	}

	@Override
	public String getTypeLabel() {
		return "Text Link";
	}

	@Override
	public String getHyperlinkText() {
		return targetName;
	}

	@Override
	public void open() {
		
		IEditorPart editor;
		try {
			editor = EditorUtil.openEditor(page, targetUri);
		} catch (PartInitException e) {
			IsabelleUIPlugin.log(e.getLocalizedMessage(), e);
			return;
		}
		
		if (editor != null) {
			EditorUtil.revealInEditor(editor, selectRegion, highlightRegion);
		}
	}

}
