package isabelle.eclipse.editors;

import java.net.URI;

import isabelle.Document.Snapshot;
import isabelle.Path;
import isabelle.eclipse.core.resource.URIPathEncoder;
import isabelle.eclipse.core.resource.URIThyLoad;
import isabelle.eclipse.core.text.DocumentModel;
import isabelle.scala.HyperlinkUtil;
import isabelle.scala.HyperlinkUtil.HyperlinkInfo;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;

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
		
		// calculate the hyperlink information from snapshot in the given offset
		Snapshot snapshot = isabelleModel.getSnapshot();
		HyperlinkInfo hyperlinkInfo = HyperlinkUtil.getHyperlink(snapshot, region.getOffset());
		if (hyperlinkInfo == null) {
			return null;
		}

		// the hyperlink target can be a loaded Isabelle document, or an external link (e.g. to the
		// heap files)
		URI targetUri;
		if (hyperlinkInfo.targetDocument() != null) {
			// document is available: it is an "internal" link to a loaded theory
			// resolve the URI from the document reference
			targetUri = URIThyLoad.resolveDocumentUri(hyperlinkInfo.targetDocument());
		} else {
			
			// try to resolve the target file (as String) into a relative/absolute URI in relation
			// to the source URI (where the hyperlink was created)
			
			// first get the URI of the source
			String sourceUriStr = isabelleModel.getName().getNode();
			URI sourceUri = URIPathEncoder.decodePath(sourceUriStr);
			
			// resolve the target URI
			Path targetPath = Path.explode(hyperlinkInfo.targetFile()); 
			URI platformUri = URIThyLoad.resolveURI(sourceUri, targetPath);
			
			// the relative path may be a workspace file (platform: URI),
			// so resolve it to file URI
			targetUri = URIThyLoad.resolvePlatformUri(platformUri.toString());
		}
		
		return new IHyperlink[] { new IsabelleHyperlink(hyperlinkInfo.linkRange(), targetUri, 
				hyperlinkInfo.targetName(), hyperlinkInfo.targetRange(),
				hyperlinkInfo.targetLine(), hyperlinkInfo.targetNameRange()) };
	}
	
}
