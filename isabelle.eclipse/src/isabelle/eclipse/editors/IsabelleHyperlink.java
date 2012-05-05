package isabelle.eclipse.editors;

import isabelle.Text.Range;
import isabelle.eclipse.IsabelleEclipsePlugin;

import java.net.URI;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

import scala.Option;

/**
 * A hyperlink to Isabelle definition (already calculated as a target URI and range). Opens a
 * corresponding file in the editor and selects target range.
 * 
 * @author Andrius Velykis
 */
public class IsabelleHyperlink implements IHyperlink {

	private final Range linkRange;
	private final URI targetUri;
	private final String targetName;
	private final Range targetRange;
	private final Range targetNameRange;
	private final int targetLine;
	
	public IsabelleHyperlink(Range linkRange, URI targetUri, String targetName,
			Range targetPosition, int targetLine, Range targetNamePosition) {
		this.linkRange = linkRange;
		this.targetUri = targetUri;
		this.targetName = targetName;
		this.targetRange = targetPosition;
		this.targetLine = targetLine;
		this.targetNameRange = targetNamePosition;
	}

	@Override
	public IRegion getHyperlinkRegion() {
		return getRegion(linkRange);
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
		
		// will open the editor on given file store
		IFileStore fileStore;
		try {
			fileStore = EFS.getStore(targetUri);
		} catch (CoreException e) {
			IsabelleEclipsePlugin.log(e.getLocalizedMessage(), e);
			return;
		}
		
		IWorkbenchPage activePage = getActivePage();
		if (activePage == null) {
			IsabelleEclipsePlugin.log("Cannot find an active workbench page", null);
			return;
		}
		
		IEditorPart editor;
		try {
			editor = IDE.openEditorOnFileStore(activePage, fileStore);
		} catch (PartInitException e) {
			IsabelleEclipsePlugin.log(e.getLocalizedMessage(), e);
			return;
		}
			
		if (editor instanceof ITextEditor) {
			revealInEditor((ITextEditor) editor, targetRange, targetNameRange, targetLine);
		}
	}

	private IWorkbenchPage getActivePage() {
		IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (activeWindow != null) {
			return activeWindow.getActivePage();
		}
		
		return null;
	}
	
	private static void revealInEditor(ITextEditor textEditor, Range targetRange,
			Range targetNameRange, int targetLine) {
		
		IDocument document = EditorUtil.getDocument(textEditor);
		
		// adapt the target position to the document
		Position pos;
		if (targetRange != null) {
			pos = restrictToLength(document, targetRange);
		} else {
			// if target position is not given (only target line is available),
			// calculate the position based on the line
			pos = getLinePosition(document, targetLine);
		}

		// set highlight range for the target position
		// (highlight is displayed on the side ruler and is revealed)
		textEditor.setHighlightRange(pos.getOffset(), pos.getLength(), true);
		
		// try also to select the target name position - that requires a text viewer
		ITextViewer viewer = EditorUtil.getTextViewer(textEditor);
		if (viewer != null) {
			
			// check if name position is available and adapt that to the document as well.
			// name position may be a sub-position of the whole target position
			Position namePos = pos;
			if (targetNameRange != null) {
				namePos = restrictToLength(document, targetNameRange);
			}
			
			try {
				// reveal and select the name range
				viewer.getTextWidget().setRedraw(false);
				viewer.revealRange(namePos.getOffset(), namePos.getLength());
				viewer.setSelectedRange(namePos.getOffset(), namePos.getLength());
			} finally {
				viewer.getTextWidget().setRedraw(true);
			}
		}
	}
	
	private static Position getLinePosition(IDocument document, int line) {
		try {
			return new Position(document.getLineOffset(line));
		} catch (BadLocationException e) {
			// no line can be determined 
			return null;
		}
	}
	
	private static Position restrictToLength(IDocument document, Range position) {
		
		if (document == null) {
			return getPosition(position);
		}
		
		Range maxRange = new Range(0, document.getLength());
		
		Option<Range> fixedRangeOpt = maxRange.try_restrict(position);
		Range fixedRange;
		if (fixedRangeOpt.isEmpty()) {
			// invalid range (e.g. outside the max range)
			// do not ignore, but better display it at (0, 0)
			fixedRange = new Range(0, 0);
		} else {
			fixedRange = fixedRangeOpt.get();
		}
		
		return getPosition(fixedRange);
	}
	
	private static Position getPosition(Range range) {
		return new Position(range.start(), range.stop() - range.start());
	}
	
	private static IRegion getRegion(Range range) {
		return new Region(range.start(), range.stop() - range.start());
	}

}
