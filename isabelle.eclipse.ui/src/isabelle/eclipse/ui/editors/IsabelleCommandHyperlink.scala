package isabelle.eclipse.ui.editors

import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.hyperlink.IHyperlink
import org.eclipse.ui.{IWorkbenchPage, PartInitException}
import org.eclipse.ui.ide.IDE

import isabelle.Command
import isabelle.eclipse.core.resource.URIThyLoad
import isabelle.eclipse.ui.IsabelleUIPlugin


/**
 * A hyperlink to Isabelle command definition.
 *
 * Resolves the command's file in the editor and selects the range.
 *
 * @param workbenchPage
 *            the page to open the editor in
 * @param linkRegion
 *            region in source editor representing the hyperlink location
 * @param targetName
 *            name of hyperlink
 * @param targetCommand
 *            target command to select when hyperlink is opened
 * @param regionInCommand
 *            a region within the target command area to be selected
 *            (e.g. a definition within a larger command)
 * 
 * 
 * @author Andrius Velykis
 */
class IsabelleCommandHyperlink(
    workbenchPage: IWorkbenchPage,
    linkRegion: IRegion,
    targetName: Option[String],
    targetCommand: Command,
    regionInCommand: Option[IRegion]) extends IHyperlink {

  override def getHyperlinkRegion(): IRegion = linkRegion
  
  override def getTypeLabel = "Isabelle Command Definition"
  
  override def getHyperlinkText(): String = targetName.orNull

  override def open() {

    // resolve target file URI from the command
    val targetUri = URIThyLoad.resolveDocumentUri(targetCommand.node_name)

    try {

      val editor = IDE.openEditor(workbenchPage, targetUri, TheoryEditor.EDITOR_ID, true)

      editor match {
        // set the command selection in the opened editor
        case theory: TheoryEditor => theory.setSelection(targetCommand, regionInCommand)

        case _ =>
      }
    } catch {
      case e: PartInitException => IsabelleUIPlugin.log(e.getMessage, e)
    }
  }
  
}
