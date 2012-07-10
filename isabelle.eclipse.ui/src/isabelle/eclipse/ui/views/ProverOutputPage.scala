package isabelle.eclipse.ui.views

import isabelle.Command
import isabelle.Isabelle_Markup
import isabelle.Markup
import isabelle.Session
import isabelle.XML
import isabelle.eclipse.core.util.SessionEvents
import isabelle.eclipse.core.util.LoggingActor
import isabelle.eclipse.ui.IsabelleImages
import isabelle.eclipse.ui.IsabelleUIPlugin
import isabelle.eclipse.ui.editors.TheoryEditor
import java.io.IOException
import java.net.URL
import org.eclipse.core.runtime.FileLocator
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Platform
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.jface.action.Action
import org.eclipse.jface.action.IAction
import org.eclipse.jface.action.GroupMarker
import org.eclipse.jface.commands.ActionHandler
import org.eclipse.jface.viewers.ISelectionChangedListener
import org.eclipse.jface.viewers.IPostSelectionProvider
import org.eclipse.jface.viewers.SelectionChangedEvent
import org.eclipse.swt.SWT
import org.eclipse.swt.browser.Browser
import org.eclipse.swt.browser.LocationAdapter
import org.eclipse.swt.browser.LocationEvent
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.ui.IActionBars
import org.eclipse.ui.ISharedImages
import org.eclipse.ui.IWorkbenchCommandConstants
import org.eclipse.ui.PlatformUI
import org.eclipse.ui.handlers.IHandlerService
import org.eclipse.ui.part.IPageSite
import org.eclipse.ui.part.Page
import org.osgi.framework.Bundle
import scala.actors.Actor._


/**
  * @author Andrius Velykis 
  */
object ProverOutputPage {
  
  private val viewId = IsabelleUIPlugin.PLUGIN_ID + ".proverOutputView"
  private val propShowTrace = viewId + ".showTrace"
  private val propLinkEditor = viewId + ".linkEditor"
  
  // init default values once
  private def prefs = IsabelleUIPlugin.getPreferences();
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
        case bad => IsabelleUIPlugin.log("Bad message received in output page: " + bad, null)
      }
    }
  }

  // subscribe to commands change session events
  override protected def sessionEvents(session: Session) = List(session.commands_changed)

  private var mainComposite: Composite = _
  private var outputArea: Browser = _

  // get the preferences value for showing the trace
  private var showTrace = prefs.getBoolean(propShowTrace)
  private var followSelection = prefs.getBoolean(propLinkEditor)
  private var currentCommand: Option[Command] = None

  private var updateJob: Job = new UpdateOutputJob(_ => None);

  // selection listener to update output when editor selection changes
  val editorListener = selectionListener { _ => updateOutputAtCaret() }
  
  // load CSS file contents, because they cannot be accessed from the browser when packaged into JARs
  private lazy val inlineCss = getCssPaths.flatMap(readResource(_)).mkString("\n\n")

  override def createControl(parent: Composite) {
    mainComposite = new Composite(parent, SWT.NONE)
    mainComposite.setLayout(new FillLayout())

    outputArea = new Browser(mainComposite, SWT.NONE)
    
    // add listener to hyperlink selection
    // the results can have hyperlinks, e.g. "sendback" to replace the command with some text, used in 'sledgehammer' resutls
    outputArea.addLocationListener(new LocationAdapter {
      override def changing(event: LocationEvent) {
        event.location match {
          // handle sendback
          case ProverOutputHtml.SendbackLink(content) => doSendback(content)
          case _ => // do nothing
        }
      }
    })

    // init session event listeners
    initSessionEvents()

    addEditorListener(editorListener)
    
    // init output on the editor position 
    updateOutputAtCaret()
  }

  override def getControl() = mainComposite

  override def setFocus() = outputArea.setFocus

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

    super.dispose()
  }

  private def updateOutputAtCaret() = {
    if (followSelection) {
      val offset = editor.getCaretPosition();
      updateOutput(_ => commandAtOffset(offset))
    }
  }

  private def updateOutput(cmdProvider: (Unit => Option[Command]), delay: Long = 0) {
    val updateJob = new UpdateOutputJob(cmdProvider)

    // cancel the previous job
    this.updateJob.cancel()
    this.updateJob = updateJob

    this.updateJob.schedule(delay)
  }

  private def commandAtOffset(offset: Int): Option[Command] = {
    val isabelleModel = Option(editor.getIsabelleModel())
    // get the command at the snapshot if the model is available
    isabelleModel flatMap { _.getSnapshot.node.command_at(offset).map(_._1) }
  }

  private def renderOutput(cmd: Command, monitor: IProgressMonitor): Option[String] = {

    // TODO do not output when invisible?
    // FIXME handle "sendback" in output_dockable.scala

    // get all command results except tracing
    val isabelleModel = Option(editor.getIsabelleModel())

    isabelleModel match {
      case None => { System.out.println("Isabelle model not available"); None }
      case Some(model) => {
        // model is available - get the results and render them
        val snapshot = model.getSnapshot();

        val filteredResults =
          snapshot.state.command_state(snapshot.version, cmd).results.iterator.map(_._2) filter {
            // FIXME not scalable
            case XML.Elem(Markup(Isabelle_Markup.TRACING, _), _) => showTrace
            case _ => true
          }

        val htmlPage = ProverOutputHtml.renderHtmlPage(filteredResults.toList, Nil, inlineCss, "IsabelleText", 12)
        Some(htmlPage)
      }
    }
  }

  private def getCssPaths(): List[URL] = {
    val bundle = Platform.getBundle(IsabelleUIPlugin.PLUGIN_ID)
    def path = getResourcePath(bundle) _

    List(path("etc/isabelle.css"), path("etc/isabelle-jedit.css")).flatten
  }

  private def getResourcePath(bundle: Bundle)(pathInBundle: String): Option[URL] = {
    val fileURL = Option(bundle.getEntry(pathInBundle));

    fileURL match {
      case Some(url) => {
        try {
          val fullURL = FileLocator.resolve(url)
          Some(fullURL)
        } catch {
          case ioe: IOException => {
            IsabelleUIPlugin.log("Unable to locate resource " + pathInBundle, ioe)
            None
          }
        }
      }
      case None => { IsabelleUIPlugin.log("Unable to locate resource " + pathInBundle, null); None }
    }
  }

  private def setContent(htmlPage: String) {
    // set the input in the UI thread
    uiExec {
      if (!outputArea.isDisposed()) {
        
        val currentPage = outputArea.getText()
        if (currentPage != htmlPage) {
          // avoid flashing the output if it is the same
          outputArea.setText(htmlPage);
        }
      }
    }
  }

  private def uiExec(f: => Unit) {
    outputArea.getDisplay.asyncExec(new Runnable {
      override def run() { f }
    })
  }

  // TODO implement for IE: http://www.quirksmode.org/dom/range_intro.html, http://help.dottoro.com/ljumcfud.php
  private def selectAllJavascript = "window.getSelection().selectAllChildren(document.body);"

  def selectAllText() {
    if (outputArea != null) {
      outputArea.execute(selectAllJavascript);
    }
  }

  def copySelectionToClipboard() {
    // do nothing at the moment - allow browser copy facilities to work
  }
  
  /** Reads the target resource URL contents */
  private def readResource(url: URL): Option[String] = {
    try {
      val source = scala.io.Source.fromURL(url, "UTF-8")
      try {
        // read text
        Some(source.mkString)
      } finally {
        source.close
      }
    } catch {
      case ioe: IOException => {
        IsabelleUIPlugin.log(ioe.getMessage, ioe)
        None
      }
    }
  }
  
  private def doSendback(text: String) {
    
    // if command and isabelle model are available, replace the current command with sendback text
    (currentCommand, Option(editor.getIsabelleModel())) match {
      case (Some(cmd), Some(isabelleModel)) => {
        val cmdOffsetOpt = isabelleModel.getSnapshot.node.command_start(cmd)
        
        cmdOffsetOpt foreach {offset => {
          // replace the command text in the document with sendback text
          editor.getDocument().replace(offset, cmd.length, text)
        }
        }
      }
      case _ =>
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

  private class UpdateOutputJob(cmdProvider: (Unit => Option[Command]))
    extends Job("Updating prover output") {
//    setPriority(Job.INTERACTIVE)

    override def run(monitor: IProgressMonitor): IStatus = {

      if (monitor.isCanceled()) {
        return Status.CANCEL_STATUS
      }

      // retrieve the current command
      currentCommand = cmdProvider()

      // render the command if available
      val result = currentCommand flatMap { cmd => renderOutput(cmd, monitor) }

      // check if cancelled - then do not set the output
      if (monitor.isCanceled()) {
        return Status.CANCEL_STATUS
      }

      // set the result if available
      result foreach setContent

      Status.OK_STATUS
    }
  }

  private class ToggleShowTraceAction extends ToggleAction("Show Trace", "Show Proof Trace", propShowTrace, showTrace) {
    setImageDescriptor(IsabelleImages.getImageDescriptor(IsabelleImages.IMG_SHOW_TRACE))
//    setDisabledImageDescriptor(null)

    override def handleValueChanged(show: Boolean) {
      showTrace = show

      // update the output with current command (refresh with new trace value)
      updateOutput(_ => currentCommand)
    }
  }
  
  /** Action to toggle linking with selection. */
  private class ToggleLinkAction extends ToggleAction("Link with Editor", "Link with Editor", propLinkEditor, followSelection) {
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
