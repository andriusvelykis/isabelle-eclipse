package isabelle.eclipse.ui.editors

import java.net.URI

import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.hyperlink.IHyperlink
import org.eclipse.ui.{IWorkbenchPage, PartInitException}

import isabelle.eclipse.ui.internal.IsabelleUIPlugin.{error, log}


/**
 * A hyperlink to a text editor. 
 * 
 * Opens the given URI and selects target region in the opened editor.
 *
 * @param workbenchPage
 *            the page to open the editor in
 * @param linkRegion
 *            region in source editor representing the hyperlink location
 * @param targetName
 *            name of hyperlink
 * @param targetUri
 *            target URI to open in an editor
 * @param selectRegion
 *            a region within the target editor to be selected
 * @param highlightRegion
 *            a region within the target editor to be highlighted
 *            (normally the whole definition, while only the name is selected)
 *
 *
 * @author Andrius Velykis
 */
class TextHyperlink(
    workbenchPage: IWorkbenchPage,
    linkRegion: IRegion,
    targetName: Option[String],
    targetUri: URI,
    selectRegion: IRegion,
    highlightRegion: IRegion) extends IHyperlink {

  override def getHyperlinkRegion(): IRegion = linkRegion

  override def getTypeLabel = "Definition Link"

  override def getHyperlinkText(): String = targetName.orNull

  override def open() {

    try {

      val editor = EditorUtil.openEditor(workbenchPage, targetUri)
      EditorUtil.revealInEditor(editor, selectRegion, highlightRegion)

    } catch {
      case e: PartInitException => log(error(Some(e)))
    }
  }

}
