package isabelle.eclipse.ui.views.outline

import org.eclipse.core.runtime.{IProgressMonitor, IStatus, Status}
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.jface.resource.{JFaceResources, LocalResourceManager}
import org.eclipse.swt.widgets.{Composite, Control}
import org.eclipse.ui.views.contentoutline.ContentOutlinePage

import isabelle.Document.Snapshot
import isabelle.Outer_Syntax
import isabelle.Thy_Syntax.Structure
import isabelle.eclipse.core.IsabelleCore
import isabelle.eclipse.core.text.DocumentModel
import isabelle.eclipse.ui.editors.TheoryEditor
import isabelle.eclipse.ui.util.SWTUtil
import isabelle.eclipse.ui.views.SessionStatusMessageArea


/**
 * Outline page for Isabelle theory files.
 * 
 * Supports both structured tree and raw markup tree.
 * 
 * @author Andrius Velykis
 */
class TheoryOutlinePage(editor: TheoryEditor) extends ContentOutlinePage {

  private var rawTree = false
  
  private val sessionStatusArea = new SessionStatusMessageArea
  private var control: Control = _
  
  
  // just so that it is not null
  @volatile private var updateJob = new OutlineParseJob(rawTree)
  
  lazy val resourceManager = new LocalResourceManager(
      JFaceResources.getResources, getTreeViewer.getControl)
  lazy val theoryStructureContent = new TheoryStructureContentProvider
  lazy val theoryStructureLabel = new TheoryStructureLabelProvider(resourceManager)
  
  lazy val theoryRawContent = new TheoryRawContentProvider
  lazy val theoryRawLabel = new TheoryRawLabelProvider(resourceManager)
  
  
  // TODO update after editing - currently it is reloaded on initialisation only
  def reload() {
    
    val newJob = new OutlineParseJob(rawTree)
    
    // cancel the previous run, in case it is still executing
    updateJob.cancel()
    newJob.schedule()
    updateJob = newJob
  }
  
  override def createControl(parent: Composite) {
    
    val (control, contentArea) = SessionStatusMessageArea.wrapPart(parent, sessionStatusArea)
    this.control = control
    
    super.createControl(contentArea)
    
    val viewer = getTreeViewer
    
    viewer.setAutoExpandLevel(2)
    reload()
  }
  
  override def getControl(): Control = control
  
  override def dispose() {
    sessionStatusArea.dispose()
    super.dispose()
  }

  private def parseTheoryStructure(syntax: Outer_Syntax,
                                   docModel: DocumentModel): TheoryStructureEntry = {
    val structure = Structure.parse(syntax, docModel.name, docModel.document.get)
    TheoryStructureEntry(syntax, structure)
  }


  private def parseTheoryRaw(snapshot: Snapshot,
                             monitor: IProgressMonitor): Iterator[TheoryRawEntry] = {

    val contentRenderer = TheoryRawLabelProvider.rawContent _
    val tooltipRenderer = TheoryRawLabelProvider.rawTooltip _

    for {
      (command, commandStart) <- snapshot.node.command_range() if !monitor.isCanceled
      tree = snapshot.state.command_state(snapshot.version, command).markup
      branchEntry <- TheoryRawEntry.branches(tree, TheoryRawEntry.Info(command, commandStart, contentRenderer, tooltipRenderer))
    } yield branchEntry
  }


  private def setInput(input: AnyRef, rawTree: Boolean) {

    val viewer = Option(getTreeViewer)

    viewer foreach { v =>
      SWTUtil.asyncExec(Some(v.getControl.getDisplay)) {

        val (contentProvider, labelProvider) =
          if (rawTree) {
            (theoryRawContent, theoryRawLabel)
          } else {
            (theoryStructureContent, theoryStructureLabel)
          }
        
        if (v.getContentProvider != contentProvider) {
          v.setContentProvider(contentProvider)
        }

        if (v.getLabelProvider != labelProvider) {
          v.setLabelProvider(labelProvider)
        }

        if (v.getInput != input) {
          v.setInput(input)
        }
      }
    }
  }


  private class OutlineParseJob(rawTree: Boolean) extends Job("Creating theory outline tree") {

    override def run(monitor: IProgressMonitor): IStatus = {

      def isCanceled: Option[Unit] = if (monitor.isCanceled) None else Some()

      def parse: Option[AnyRef] =
        (rawTree, editor.isabelleModel, IsabelleCore.isabelle.recentSyntax) match {

          case (true, Some(docModel), _) =>
            Some(parseTheoryRaw(docModel.snapshot, monitor))

          case (false, Some(docModel), Some(syntax)) =>
            Some(parseTheoryStructure(syntax, docModel))

          case _ => None
        }

      // need to sleep, otherwise markup information may not yet be available
      // TODO listen to markup changes
      if (rawTree) {
        Thread.sleep(2000)
      }

      val status = for {
        _ <- isCanceled
        input <- parse
        _ <- isCanceled
      } yield {
        setInput(input, rawTree)
        Status.OK_STATUS
      }

      status getOrElse Status.CANCEL_STATUS
    }
  }
  
}
