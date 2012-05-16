package isabelle.eclipse.ui.editors;

import isabelle.Command;
import isabelle.eclipse.core.resource.URIThyLoad;
import isabelle.eclipse.ui.IsabelleUIPlugin;
import isabelle.scala.DocumentRef;

import java.net.URI;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;


/**
 * A hyperlink to Isabelle command definition. Resolves the command's file in the editor and selects
 * the range.
 * 
 * @author Andrius Velykis
 */
public class IsabelleHyperlink implements IHyperlink {

	private final IWorkbenchPage page;
	private final IRegion linkRegion;
	private final Command targetCommand;
	private final IRegion regionInCommand;
	private final String targetName;
	
	/**
	 * Creates the hyperlink for the given command.
	 * 
	 * @param page
	 *            the page to open the editor in
	 * @param linkRegion
	 *            region in source editor representing the hyperlink location
	 * @param targetCommand
	 *            target command to select when hyperlink is opened
	 * @param regionInCommand
	 *            a region within the target command area to be selected (e.g. a definition within a
	 *            larger command)
	 * @param targetName
	 *            name of hyperlink
	 */
	public IsabelleHyperlink(IWorkbenchPage page, IRegion linkRegion, Command targetCommand,
			IRegion regionInCommand, String targetName) {
		this.page = page;
		this.linkRegion = linkRegion;
		this.targetCommand = targetCommand;
		this.regionInCommand = regionInCommand;
		this.targetName = targetName;
	}

	@Override
	public IRegion getHyperlinkRegion() {
		return linkRegion;
	}

	@Override
	public String getTypeLabel() {
		return "Isabelle Link";
	}

	@Override
	public String getHyperlinkText() {
		return targetName;
	}

	@Override
	public void open() {
		
		// resolve target file URI from the command
		URI targetUri = URIThyLoad.resolveDocumentUri(targetCommand.node_name());
		
		TheoryEditor editor;
		try {
			// open in theory editor
			editor = (TheoryEditor) IDE.openEditor(page, targetUri, TheoryEditor.EDITOR_ID, true);
		} catch (PartInitException e) {
			IsabelleUIPlugin.log(e.getLocalizedMessage(), e);
			return;
		}
		
		// set the command selection in the opened editor
		editor.setSelection(targetCommand, regionInCommand);
	}
}
