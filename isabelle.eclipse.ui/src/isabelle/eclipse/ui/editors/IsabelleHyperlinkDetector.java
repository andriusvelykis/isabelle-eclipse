package isabelle.eclipse.ui.editors;

import java.net.URI;

import isabelle.Document.Snapshot;
import isabelle.Path;
import isabelle.Text.Range;
import isabelle.eclipse.core.resource.URIPathEncoder;
import isabelle.eclipse.core.resource.URIThyLoad;
import isabelle.eclipse.core.text.DocumentModel;
import isabelle.scala.HyperlinkUtil;
import isabelle.scala.HyperlinkUtil.CommandHyperlinkInfo;
import isabelle.scala.HyperlinkUtil.HyperlinkInfo;
import isabelle.scala.HyperlinkUtil.TextHyperlinkInfo;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;


/**
 * A detector for Isabelle definition hyperlinks (commands to their definitions).
 * <p>
 * Detects hyperlinks from Isabelle model snapshot (when Isabelle is running).
 * </p>
 * 
 * @author Andrius Velykis
 */
public class IsabelleHyperlinkDetector extends AbstractHyperlinkDetector {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.text.hyperlink.IHyperlinkDetector#detectHyperlinks(
	 * 		org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion, boolean)
	 */
	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region,
			boolean canShowMultipleHyperlinks) {
		
		// get the document via IAdaptable (requires the context, e.g. TheoryEditor, to be
		// available)
		DocumentModel isabelleModel = (DocumentModel) getAdapter(DocumentModel.class);
		if (isabelleModel == null) {
			return null;
		}
		
		// retrieve the editor and its page to open hyperlinks in
		IEditorPart editor = (IEditorPart) getAdapter(IEditorPart.class);
		if (editor == null) {
			return null;
		}
		
		if (editor.getSite() == null) {
			return null;
		}
		IWorkbenchPage page = editor.getSite().getPage();
		
		// calculate the hyperlink information from snapshot in the given offset
		Snapshot snapshot = isabelleModel.getSnapshot();
		HyperlinkInfo hyperlinkInfo = HyperlinkUtil.getHyperlink(snapshot, region.getOffset(), 1);
		if (hyperlinkInfo == null) {
			return null;
		}
		
		// create the hyperlink
		IHyperlink hyperlink = createHyperlink(page, isabelleModel, hyperlinkInfo);
		if (hyperlink == null) {
			return null;
		}
		
		return new IHyperlink[] { hyperlink };
	}
	
	/**
	 * Creates hyperlink from the given information.
	 * 
	 * @param page
	 * @param isabelleModel
	 * @param info
	 * @return
	 */
	private IHyperlink createHyperlink(IWorkbenchPage page, DocumentModel isabelleModel, HyperlinkInfo info) {
		if (info instanceof CommandHyperlinkInfo) {
			CommandHyperlinkInfo cmdInfo = (CommandHyperlinkInfo) info;
			
			// create command hyperlink, which determines the target editor/location from the command
			return new IsabelleHyperlink(page, getRegion(info.linkRange()),
					cmdInfo.targetCommand(), getRegion(cmdInfo.rangeInCommand()), info.linkName());
		}
		
		if (info instanceof TextHyperlinkInfo) {
			TextHyperlinkInfo textInfo = (TextHyperlinkInfo) info;
			IRegion targetRegion = getRegion(textInfo.targetRange());
			
			// create text hyperlink with calculated information about target URI/location
			return new TextHyperlink(page, getRegion(info.linkRange()), 
					resolveURI(isabelleModel, textInfo.targetFile()), 
					info.linkName(), targetRegion, targetRegion);
		}
		
		return null;
	}
	
	private static IRegion getRegion(Range range) {
		return new Region(range.start(), range.stop() - range.start());
	}
	
	private URI resolveURI(DocumentModel isabelleModel, String filePath) {
		// try to resolve the target file (as String) into a relative/absolute URI in relation
		// to the source URI (where the hyperlink was created)
		
		// first get the URI of the source
		String sourceUriStr = isabelleModel.getName().getNode();
		URI sourceUri = URIPathEncoder.decodePath(sourceUriStr, false);
		
		// resolve the target URI
		Path targetPath = Path.explode(filePath); 
		URI platformUri = URIThyLoad.resolveURI(sourceUri, targetPath, URIThyLoad.resolveURI$default$3());
		
		// the relative path may be a workspace file (platform: URI),
		// so resolve it to file URI
		return URIThyLoad.resolvePlatformUri(platformUri.toString());
	}
}
