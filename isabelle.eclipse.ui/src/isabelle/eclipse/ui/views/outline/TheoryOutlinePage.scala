package isabelle.eclipse.ui.views.outline

import scala.actors.Actor._

import org.eclipse.core.runtime.{IProgressMonitor, IStatus, Status}
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.jface.action.Action
import org.eclipse.jface.resource.{JFaceResources, LocalResourceManager}
import org.eclipse.jface.text.{DocumentEvent, IDocumentListener, IRegion, ITextViewer, Region}
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.{Composite, Control}
import org.eclipse.ui.IActionBars
import org.eclipse.ui.views.contentoutline.ContentOutlinePage

import isabelle.{Outer_Syntax, Session}
import isabelle.Document.Snapshot
import isabelle.Thy_Syntax.Structure
import isabelle.eclipse.core.IsabelleCore
import isabelle.eclipse.core.text.DocumentModel
import isabelle.eclipse.core.util.{LoggingActor, SessionEvents}
import isabelle.eclipse.ui.IsabelleImages
import isabelle.eclipse.ui.editors.TheoryEditor
import isabelle.eclipse.ui.preferences.IsabelleUIPreferences
import isabelle.eclipse.ui.text.DocumentListenerSupport
import isabelle.eclipse.ui.util.{SWTUtil, TypingDelayHelper}
import isabelle.eclipse.ui.views.SessionStatusMessageArea


/**
 * Outline page for Isabelle theory files.
 *
 * Supports both structured tree and raw markup tree.
 *
 * @author Andrius Velykis
 */
class TheoryOutlinePage(editor: TheoryEditor, editorViewer: => ITextViewer)
    extends ContentOutlinePage with SessionEvents {

  private var rawTree = true
  
  private var control: Control = _
  
  private val delayHelper = new TypingDelayHelper
  private val documentListener = new DocumentListenerSupport(new IDocumentListener {
    
    override def documentAboutToBeChanged(event: DocumentEvent) {}
    override def documentChanged(event: DocumentEvent) = 
      if (!rawTree) reloadWithDelay()
  })
  
  
  // the actor to react to session events (for Raw tree)
  override protected val sessionActor = LoggingActor {
    loop {
      react {
        case changed: Session.Commands_Changed if (rawTree) => 
          editor.isabelleModel foreach { model =>
            // avoid updating Raw tree if commands are from a different document
            if (changed.nodes contains model.name) {
              reloadWithDelay()
            }
          }
        }
      }
    }

  // subscribe to commands change session events (for Raw tree)
  override protected def sessionEvents(session: Session) = List(session.commands_changed)
  
  
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
  
  def reloadWithDelay() =
    controlSafe foreach (c => delayHelper.scheduleCallback(Some(c.getDisplay))(reload))
  
  override def createControl(parent: Composite) {
    
    val (control, contentArea) = SessionStatusMessageArea.wrapPart(parent)
    this.control = control
    
    super.createControl(contentArea)
    
    val viewer = getTreeViewer
    viewer.setAutoExpandLevel(2)
    
    // register actions
    registerToolbarActions(getSite.getActionBars)
    
    documentListener.init(editorViewer)
    initSessionEvents()
    reload()
  }
  
  private def registerToolbarActions(actionBars: IActionBars) {
    val toolBarManager = actionBars.getToolBarManager
    toolBarManager.add(new RawTreeAction)
  }
  
  override def getControl(): Control = control
  
  private def controlSafe(): Option[Control] = Option(getControl) filterNot (_.isDisposed)
  
  override def dispose() {
    disposeSessionEvents()
    documentListener.dispose()
    delayHelper.stop()
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


  def selectedRegionInEditor: Option[IRegion] = getSelection match {
    case ss: IStructuredSelection => ss.getFirstElement match {
      case struct: TheoryStructureEntry => Some(new Region(struct.offset, struct.entry.length))
      case raw: TheoryRawEntry => Some(new Region(raw.range.start, raw.range.stop - raw.range.start))
      case _ => None
    }
    case _ => None
  }


  private class OutlineParseJob(rawTree: Boolean) extends Job("Creating theory outline tree") {
    // low priority job
    setPriority(Job.DECORATE)

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

  private class RawTreeAction extends Action("Show Raw Tree", SWT.TOGGLE) {

    setToolTipText("Show Raw Tree")
//    setDescription("?")
    setImageDescriptor(IsabelleImages.RAW_TREE)

    {
      val prefRawTree = IsabelleUIPreferences.getBoolean(prefKey, false)
      setRawTree(prefRawTree, true)
    }

    def prefKey = IsabelleUIPreferences.OUTLINE_RAW_TREE

    override def run() {
      setRawTree(!rawTree)
      reload()
    }

    def setRawTree(isRaw: Boolean, init: Boolean = false) {
      rawTree = isRaw
      setChecked(isRaw)

      IsabelleUIPreferences.prefs.putBoolean(prefKey, isRaw)
    }
  }
  
}
