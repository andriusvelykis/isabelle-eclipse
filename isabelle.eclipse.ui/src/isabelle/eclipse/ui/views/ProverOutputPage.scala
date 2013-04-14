package isabelle.eclipse.ui.views

import scala.actors.Actor._

import org.eclipse.core.runtime.{IProgressMonitor, IStatus, Status}
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.jface.action.{Action, GroupMarker, IAction}
import org.eclipse.jface.commands.ActionHandler
import org.eclipse.jface.text.Document
import org.eclipse.jface.text.source.AnnotationModel
import org.eclipse.jface.viewers.{IPostSelectionProvider, ISelectionChangedListener, SelectionChangedEvent}
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.{Composite, Control}
import org.eclipse.ui.{IActionBars, ISharedImages, IWorkbenchCommandConstants, PlatformUI}
import org.eclipse.ui.handlers.IHandlerService
import org.eclipse.ui.part.{IPageSite, Page}

import isabelle.{Future, Linear_Set, Pretty, Protocol, Session, Text, XML}
import isabelle.Command
import isabelle.Document.Snapshot
import isabelle.eclipse.core.util.{LoggingActor, SessionEvents}
import isabelle.eclipse.ui.annotations.IsabelleAnnotations
import isabelle.eclipse.ui.editors.{IsabellePartitions, IsabelleTheorySourceViewer, TheoryEditor}
import isabelle.eclipse.ui.internal.{IsabelleImages, IsabelleUIPlugin}
import isabelle.eclipse.ui.internal.IsabelleUIPlugin.{error, log}
import isabelle.eclipse.ui.util.SWTUtil


/**
  * @author Andrius Velykis 
  */
object ProverOutputPage {
  
  private val viewId = IsabelleUIPlugin.plugin.pluginId + ".proverOutputView"
  private val propShowTrace = viewId + ".showTrace"
  private val propLinkEditor = viewId + ".linkEditor"
  
  // init default values once
  private def prefs = IsabelleUIPlugin.plugin.getPreferenceStore
  prefs.setDefault(propShowTrace, false);
  prefs.setDefault(propLinkEditor, true);
  
}

/**
  * @author Andrius Velykis 
  */
class ProverOutputPage(val editor: TheoryEditor) extends Page with SessionEvents {

  // import object contents to avoid full name referencing
  import ProverOutputPage._
  
  // the actor to react to session events
  override protected val sessionActor = LoggingActor {
    loop {
      react {
        case changed: Session.Commands_Changed => {

          // check if current command is among the changed ones
          val cmd = currentCommand filter { changed.commands.contains }

          if (cmd.isDefined) {
            // the command has changed - update using it
            updateOutput(_ => cmd)
          }
        }
        case bad => log(error(msg = Some("Bad message received in output page: " + bad)))
      }
    }
  }

  // subscribe to commands change session events
  override protected def sessionEvents(session: Session) = List(session.commands_changed)

  private var control: Control = _
  private var outputViewer: IsabelleTheorySourceViewer = _

  // get the preferences value for showing the trace
  private var showTrace = prefs.getBoolean(propShowTrace)
  private var followSelection = prefs.getBoolean(propLinkEditor)
  
  @volatile private var currentCommand: Option[Command] = None
  @volatile private var currentResultsSnapshot: Option[Snapshot] = None

  private var updateJob: Job = new UpdateOutputJob(_ => None, showTrace);

  // selection listener to update output when editor selection changes
  val editorListener = selectionListener { _ => updateOutputAtCaret() }
  

  override def createControl(parent: Composite) {
    
    val (control, contentArea) = SessionStatusMessageArea.wrapPart(parent, getSite)
    this.control = control
    
    outputViewer = createSourceViewer(
        contentArea,
        editor.isabelleModel map (_.session),
        currentResultsSnapshot,
        Some(editor))


    // init session event listeners
    initSessionEvents()

    addEditorListener(editorListener)
    
    // init output on the editor position 
    updateOutputAtCaret()
  }

  private def createSourceViewer(parent: Composite,
                                 session: => Option[Session],
                                 snapshot: => Option[Snapshot],
                                 targetEditor: => Option[TheoryEditor]): IsabelleTheorySourceViewer = {

    val styles = SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | /*SWT.BORDER | */ SWT.FULL_SELECTION

    val viewer = IsabelleTheorySourceViewer(parent, session, snapshot, targetEditor, styles)
    viewer.setEditable(false)

    val doc = new Document with IsabellePartitions
    val annotationModel = new AnnotationModel with IsabelleAnnotations {
      override val document = doc
    }
    annotationModel.connect(doc)
    
    viewer.setDocument(doc, annotationModel)
    viewer.showAnnotations(true)

    viewer
  }


  override def getControl() = control

  override def setFocus() = outputViewer.getControl.setFocus

  override def init(pageSite: IPageSite) {
    super.init(pageSite)
    
    registerToolbarActions(pageSite.getActionBars)
  }

  private def registerToolbarActions(actionBars: IActionBars) {
    
    // toggle link is also a handler for "link with editor" workbench action
    val toggleLinkAction = new ToggleLinkAction();
    toggleLinkAction.setActionDefinitionId(IWorkbenchCommandConstants.NAVIGATE_TOGGLE_LINK_WITH_EDITOR);
    
    // add actions to toolbar
    Option(actionBars.getToolBarManager()) foreach { mgr =>
      {
        // also add group markers to allow command-based additions
        mgr.add(new GroupMarker("info-view"))
        mgr.add(new ToggleShowTraceAction())
        mgr.add(new GroupMarker("view"))
        mgr.add(toggleLinkAction)
      }
    }
    
    // register as the handler
    val handlerService = getSite().getService(classOf[IHandlerService]).asInstanceOf[IHandlerService];
    handlerService.activateHandler(
        IWorkbenchCommandConstants.NAVIGATE_TOGGLE_LINK_WITH_EDITOR,
        new ActionHandler(toggleLinkAction));

  }

  override def dispose() {

    removeEditorListener(editorListener)
    disposeSessionEvents()

    outputViewer.dispose()
    
    super.dispose()
  }

  private def updateOutputAtCaret() = {
    if (followSelection) {
      val offset = editor.caretPosition
      updateOutput(_ => commandAtOffset(offset))
    }
  }

  private def updateOutput(cmdProvider: (Unit => Option[Command]), delay: Long = 0) {
    val updateJob = new UpdateOutputJob(cmdProvider, showTrace)

    // cancel the previous job
    this.updateJob.cancel()
    this.updateJob = updateJob

    this.updateJob.schedule(delay)
  }

  private def commandAtOffset(offset: Int): Option[Command] = {
    // get the command at the snapshot if the model is available
    editor.isabelleModel flatMap { _.snapshot.node.command_at(offset).map(_._1) }
  }

  private def renderOutput(cmd: Command,
                           showTrace: Boolean,
                           monitor: IProgressMonitor): Option[(String, Snapshot)] = {

    // TODO do not output when invisible?

    editor.isabelleModel match {
      case None => { System.out.println("Isabelle model not available"); None }
      case Some(model) => {
        // model is available - get the results and render them
        val snapshot = model.snapshot
        
        val cmdState = snapshot.state.command_state(snapshot.version, cmd)
        val resultsMarkup = commandStateMarkup(cmdState, showTrace)
        
        // TODO compare resultsMarkup with current before rendering to avoid replacement?

        val separateMessagesMarkup = Pretty.separate(resultsMarkup)
        // TODO implement formatting based on view size
        val outputWidth = 100
        val formattedMarkup = Pretty.formatted(separateMessagesMarkup, outputWidth)//, Pretty_UI.font_metric(fm))
        
        val (text, state) = renderDocument(snapshot, cmdState.results, formattedMarkup)
        Some(text, state)
      }
    }
  }


  private def commandStateMarkup(st: Command.State, showTrace: Boolean): List[XML.Tree] = {
    val msgs = st.results.entries.map(_._2).filterNot(Protocol.is_result)
    val traceFiltered = if (showTrace) {
      msgs
    } else {
      msgs.filterNot(Protocol.is_tracing)
    }
    
    traceFiltered.toList
  }

  private def renderDocument(base_snapshot: isabelle.Document.Snapshot,
                             base_results: Command.Results,
                             formatted_body: XML.Body): (String, isabelle.Document.Snapshot) = {

    val command = Command.rich_text(isabelle.Document.new_id(), base_results, formatted_body)
    val node_name = command.node_name
    val edits: List[isabelle.Document.Edit_Text] =
      List(node_name -> isabelle.Document.Node.Edits(List(Text.Edit.insert(0, command.source))))

    val state0 = base_snapshot.state.define_command(command)
    val version0 = base_snapshot.version
    val nodes0 = version0.nodes

    val nodes1 = nodes0 + (node_name -> nodes0(node_name).update_commands(Linear_Set(command)))
    val version1 = isabelle.Document.Version.make(version0.syntax, nodes1)
    val state1 =
      state0.continue_history(Future.value(version0), edits, Future.value(version1))._2
        .define_version(version1, state0.the_assignment(version0))
        .assign(version1.id, List(command.id -> Some(isabelle.Document.new_id())))._2

    (command.source, state1.snapshot())
  }
  

  private def setContent(resultsText: String, snapshot: Snapshot) {
    // set the input in the UI thread
    SWTUtil.asyncUnlessDisposed(Option(outputViewer.getControl)) {
      this.currentResultsSnapshot = Some(snapshot)
      outputViewer.getDocument.set(resultsText)
      outputViewer.updateAnnotations()
    }
  }


  private def selectionListener(f: (SelectionChangedEvent => Unit)) =
    new ISelectionChangedListener {
      override def selectionChanged(event: SelectionChangedEvent) {
        f(event)
      }
    }

  private def addEditorListener(listener: ISelectionChangedListener) {
    editor.getSelectionProvider match {
      case post: IPostSelectionProvider => post.addPostSelectionChangedListener(listener)
      case normal => normal.addSelectionChangedListener(listener)
    }
  }

  private def removeEditorListener(listener: ISelectionChangedListener) {
    editor.getSelectionProvider match {
      case post: IPostSelectionProvider => post.removePostSelectionChangedListener(listener)
      case normal => normal.removeSelectionChangedListener(listener)
    }
  }

  private class UpdateOutputJob(cmdProvider: (Unit => Option[Command]), showTrace: Boolean)
    extends Job("Updating prover output") {
//    setPriority(Job.INTERACTIVE)

    override def run(monitor: IProgressMonitor): IStatus = {

      if (monitor.isCanceled()) {
        return Status.CANCEL_STATUS
      }

      // retrieve the current command
      currentCommand = cmdProvider()

      // render the command if available
      val result = currentCommand flatMap { cmd => renderOutput(cmd, showTrace, monitor) }

      // check if cancelled - then do not set the output
      if (monitor.isCanceled()) {
        return Status.CANCEL_STATUS
      }

      // set the result if available
      result foreach Function.tupled(setContent)

      Status.OK_STATUS
    }
  }

  private class ToggleShowTraceAction
      extends ToggleAction("Show Trace", "Show Proof Trace",
                           propShowTrace, showTrace) {

    setImageDescriptor(IsabelleImages.SHOW_TRACE)
//    setDisabledImageDescriptor(null)

    override def handleValueChanged(show: Boolean) {
      showTrace = show

      // update the output with current command (refresh with new trace value)
      updateOutput(_ => currentCommand)
    }
  }

  /** Action to toggle linking with selection. */
  private class ToggleLinkAction
      extends ToggleAction("Link with Editor", "Link with Editor",
                           propLinkEditor, followSelection) {

    private def images = PlatformUI.getWorkbench().getSharedImages();
    setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED));
    setDisabledImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED_DISABLED));

    override def handleValueChanged(link: Boolean) {
      followSelection = link

      // update the output with the current editor position (will do nothing if following disabled)
      updateOutputAtCaret()
    }
  }
  
  private abstract class ToggleAction(text: String, desc: String, prefKey: String, initVal: Boolean)
    extends Action(text, IAction.AS_CHECK_BOX) {

//    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, TOGGLE_LINK_ID)
    setToolTipText(desc)
    setDescription(desc)
    setChecked(initVal)

    override def run() {
      setChecked(isChecked());
      valueChanged(isChecked());
    }

    private def valueChanged(value: Boolean) {
      // set new value in the preferences
      prefs.setValue(prefKey, value)
      
      // delegate to the implementation
      handleValueChanged(value)
    }
    
    protected def handleValueChanged(value: Boolean): Unit

  }

}
