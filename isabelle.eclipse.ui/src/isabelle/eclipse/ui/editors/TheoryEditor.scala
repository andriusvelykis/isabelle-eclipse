package isabelle.eclipse.ui.editors

import java.net.{URI, URISyntaxException}

import scala.actors.Actor._
import scala.collection.JavaConverters._

import org.eclipse.core.filesystem.EFS
import org.eclipse.core.runtime.CoreException
import org.eclipse.jface.dialogs.MessageDialog
import org.eclipse.jface.resource.{JFaceResources, LocalResourceManager}
import org.eclipse.jface.text.{IDocument, IRegion, ITextViewerExtension2, Region}
import org.eclipse.jface.text.source.IAnnotationModel
import org.eclipse.jface.viewers.{ISelectionChangedListener, SelectionChangedEvent}
import org.eclipse.swt.widgets.Composite
import org.eclipse.ui.{IEditorInput, IEditorSite, PartInitException}
import org.eclipse.ui.contexts.IContextService
import org.eclipse.ui.editors.text.TextEditor
import org.eclipse.ui.views.contentoutline.IContentOutlinePage

import isabelle.{Command, Document, Session, Thy_Header, Thy_Info}
import isabelle.Text.Range
import isabelle.eclipse.core.IsabelleCore
import isabelle.eclipse.core.app.Isabelle
import isabelle.eclipse.core.resource.URIThyLoad._
import isabelle.eclipse.core.text.{DocumentModel, EditDocumentModel, IsabelleDocument, ReadOnlyDocumentModel}
import isabelle.eclipse.core.util.AdapterUtil.adapt
import isabelle.eclipse.core.util.LoggingActor
import isabelle.eclipse.ui.editors.EditorUtil2.preserveScroll
import isabelle.eclipse.ui.internal.IsabelleImages
import isabelle.eclipse.ui.internal.IsabelleUIPlugin.{error, log}
import isabelle.eclipse.ui.util.JobUtil.uiJob
import isabelle.eclipse.ui.util.ResourceUtil
import isabelle.eclipse.ui.util.SWTUtil.asyncUnlessDisposed
import isabelle.eclipse.ui.views.outline.TheoryOutlinePage


/** The editor for Isabelle theory files.
  * 
  * Editor is backed by a Isabelle DocumentModel, which ensures sychronisation with the prover.
  * When the prover is available, the Isabelle editor #state is created, which adds functionality
  * that requires the active prover.
  *  
  * @author Andrius Velykis
  */
object TheoryEditor {

  val EDITOR_ID = "isabelle.eclipse.ui.theoryEditor"
  val EDITOR_SCOPE = "isabelle.eclipse.ui.theoryEditorScope"

}

class TheoryEditor extends TextEditor {

  import TheoryEditor._

  /** Resource manager for shared system resources that need to be disposed (e.g. colours) */
  val resourceManager = new LocalResourceManager(JFaceResources.getResources)

  /** The Isabelle-state of the editor (available after Isabelle is launched) */
  private var state: Option[State] = None
  private var init = false

  /** A flag to indicate that Isabelle document needs initialisation (do symbols) */
  @volatile private var needDocumentInit = false
  
  private var editable = true

  val outlinePage = new TheoryOutlinePage(this, getSourceViewer)
  // listen to outline page selection changes and highlight the selection in editor
  outlinePage.addSelectionChangedListener(new ISelectionChangedListener {
    override def selectionChanged(event: SelectionChangedEvent) =
      outlinePage.selectedRegionInEditor foreach (r => selectInEditor(r, r))
  })


  private val systemListener = LoggingActor {
    loop {
      react {
        // upon system init, initialise editor document - the symbols have changed then
        case Isabelle.SystemInit => {
          needDocumentInit = true
          uiJob("Initialising Isabelle Document") { initDocument() }
        }
        // when session is init/shutdown, update the editor state accordingly
        case Isabelle.SessionInit(session) => uiJob("Initialising Isabelle Editor") { initState(session) }
        case Isabelle.SessionShutdown(_) => uiJob("Updating Isabelle Editor") { disposeState() }
        case _ =>
      }
    }
  }

  {
    setSourceViewerConfiguration(new IsabelleTheoryConfiguration(this, resourceManager))
    setDocumentProvider(new IsabelleFileDocumentProvider)
  }

  @throws(classOf[PartInitException])
  override def init(site: IEditorSite, input: IEditorInput) {

    super.init(site, input)
    activateContext()

    init = true;
  }

  /**
   * Activate a context that this view uses. It will be tied to this editor activation events
   * and will be removed when the editor is disposed.
   */
  private def activateContext() {
    val service = adapt(getSite.getService _)(classOf[IContextService])
    service.foreach(_.activateContext(EDITOR_SCOPE))
  }

  @throws(classOf[CoreException])
  override protected def doSetInput(input: IEditorInput) {

    // disconnect the old state
    disposeState()

    super.doSetInput(input)

    if (init) {
      // connect & init state with the current session and the new document
      IsabelleCore.isabelle.session.foreach(initState(_, input))
    }
  }

  override def createPartControl(parent: Composite) {
    super.createPartControl(parent)

    // add listener to the isabelle app to react to session init
    val isabelle = IsabelleCore.isabelle
    isabelle.systemEvents += systemListener

    // init state if session is already available
    isabelle.session.foreach(initState(_))
  }

  private def createDocumentName(input: IEditorInput): Option[Document.Node.Name] = {

    // resolve document URI from input and then get the document name from URI
    // (needs to conform to specific pattern, as specified in Thy_Header)
    inputURI(input) flatMap { uri =>
      {

        val name = Thy_Header.thy_name(uri.toString) map { URINodeName(uri, _) }
        if (name.isEmpty) {
          log(error(msg = Some("Cannot resolve theory name for URI: " + uri.toString)))
        }

        name map toDocumentNodeName
      }
    }
  }

  private def inputURI(input: IEditorInput): Option[URI] = {
    try {
      ResourceUtil.getInputURI(input)
    } catch {
      case e: URISyntaxException => {
        log(error(Some(e)))
        None
      }
    }
  }

  private def initState(session: Session, input: IEditorInput = getEditorInput) {

    initDocument()
    
    val name = createDocumentName(input)
    
    val loaded = name forall (n => session.thy_load.loaded_theories(n.theory))
    if (loaded) {
      makeReadOnly()
    }

    val docModel = name map { n =>
      if (loaded) new ReadOnlyDocumentModel(session, document, n)
      else new EditDocumentModel(session, document, n)
    }

    state = docModel map (new State(_))
    state.foreach(_.init())

    reloadOutline()
  }


  private def makeReadOnly() {
    // use loaded file icon for the editor
    setTitleImage(resourceManager.createImageWithDefault(IsabelleImages.ISABELLE_LOADED_FILE))
    // disable editing as well
    editable = false
    getSourceViewer.setEditable(isEditable())
  }
  
  override def isEditable() = editable && super.isEditable()


  def document: IDocument = getDocumentProvider.getDocument(getEditorInput)
  
  private[editors] def annotationModel: Option[IAnnotationModel] = 
    Option(getDocumentProvider) flatMap (p => Option(p.getAnnotationModel(getEditorInput)))

  def isabelleModel: Option[DocumentModel] = state.map(_.isabelleModel)


  /**
   * Initialise document symbols
   */
  private def initDocument() = if (needDocumentInit) {
    document match {
      case isaDoc: IsabelleDocument => preserveScroll(getSourceViewer) {
        isaDoc.init()
        // also reset undo manager, because if there are undo actions,
        // they mess up encoded/decoded appearance
        getSourceViewer.resetPlugins
      }
      case _ =>
    }
    needDocumentInit = false
  }


  private def reloadOutline() = outlinePage.reload()


  override def dispose() {

    IsabelleCore.isabelle.systemEvents -= systemListener

    // TODO review what happens if a second editor is opened for the same input
    disposeState()
    resourceManager.dispose()
    super.dispose()
  }

  private def disposeState() {
    state.foreach(_.dispose())
    state = None
  }


  override def getAdapter(adapter: Class[_]): AnyRef = {

    val value =
      if (classOf[IContentOutlinePage] == adapter) {
        Some(outlinePage)

      } else if (classOf[DocumentModel] == adapter) {
        // expose the Isabelle model via IAdaptable
        state.map(_.isabelleModel)

      } else {
        None
      }

    value.getOrElse(super.getAdapter(adapter))
  }


  def caretPosition: Int = getSourceViewer.getTextWidget.getCaretOffset

  def submitToCaret() = state foreach { _ =>
    MessageDialog.openInformation(getSite.getShell, "Functionality Disabled",
      "Submitting to selection is currently disabled in favour of submitting active view.")
  }

  /**
   * Selects and reveals the range in the editor.
   *
   * @param selectRange  range to be selected
   * @param highlightRange  range to be highlighted (e.g. with range annotation)
   */
  def selectInEditor(selectRange: IRegion, highlightRange: IRegion) {

    setHighlightRange(highlightRange.getOffset, highlightRange.getLength, true)
    selectAndReveal(selectRange.getOffset, selectRange.getLength,
      highlightRange.getOffset, highlightRange.getLength)

    // TODO select in outline as well?
  }

  /**
   * Selects the command in the editor.
   *
   * @param command  the command to be selected, must be in the snapshot
   * @param regionInCommand  region within the command to be selected, or `None` if the
   *                         whole command is to be selected
   */
  def setSelection(command: Command, regionInCommand: Option[IRegion]) {

    // find the command in the snapshot
    val commandStart = isabelleModel.flatMap(_.snapshot.node.command_start(command))

    // get the full command range
    commandStart.map(new Region(_, command.length)) foreach { cmdRange =>
      {
        val rangeInCommand = regionInCommand.map(range =>
          new Region(cmdRange.getOffset + range.getOffset, range.getLength))

        // reveal & select
        selectInEditor(rangeInCommand.getOrElse(cmdRange), cmdRange)
      }
    }
  }


  /* The prover-active state */

  /** The prover state of the editor (and associated components that require the prover to be running) */
  private class State(val isabelleModel: DocumentModel) extends DocumentPerspectiveTracker {

    // When commands change (e.g. results from the prover), signal to update the document view,
    // e.g. new markups, etc.
    val commandChange = new CommandChangeHelper(isabelleModel)(refreshViewUI)

    override protected def textViewer = getSourceViewer
    
    private def control = Option(getSourceViewer) flatMap (v => Option(v.getTextWidget))

    val markers = new TheoryViewerAnnotations(
      Some(isabelleModel.snapshot),
      isabelleModel.document,
      annotationModel,
      Option(EditorUtil.getResource(getEditorInput)))

    def init() {
      
      isabelleModel.init()
      commandChange.init()
      
      initPerspective()

      loadTheoryImports()
      
      // refresh all after initialisation
      refreshView()
    }

    def dispose() {
      commandChange.dispose()
      disposePerspective()
    }

    def refreshViewUI(changedRanges: Option[List[Range]] = None) =
      asyncUnlessDisposed(control)(refreshView(changedRanges))

    /**
     * Refreshes the text presentation.
     * 
     * Must be called in the UI thread.
     */
    def refreshView(changedRanges: Option[List[Range]] = None) {

      // regenerate annotations for the changed ranges
      markers.updateAnnotations(changedRanges)

      // After setting annotations, queue viewer refresh in the UI thread.
      // 
      // This is necessary because there seems to be a race condition with annotation model
      // when some refreshes are being lost, so doing it explicitly here.
      // AnnotationRepainter schedules refresh in UI thread using asyncExec, so add one more after
      asyncUnlessDisposed(control) {
        (Option(getSourceViewer), changedRanges) match {
          case (Some(textViewer: ITextViewerExtension2), Some(ranges)) =>
            ranges foreach (r => textViewer.invalidateTextPresentation(r.start, r.length))

          case (Some(viewer), _) => viewer.invalidateTextPresentation()

          case _ =>
        }
      }
    }

    /**
     * Loads imported theories to Isabelle. Initialises document models for each of the dependency
     * but does not open new editors.
     *
     * TODO ask to open new editors as in jEdit?
     */
    private def loadTheoryImports() {

      val nodes = pendingDependencies()

      nodes.foreach(node => {

        // resolve document URI to load the file contents
        val uri = resolveDocumentUri(node)

        val fileStore =
          try {
            Some(EFS.getStore(uri))
          } catch {
            case e: CoreException => {
              // cannot load parent - skip
              log(error(Some(e)))
              None
            }
          }

        // create the editor input to feed to document provider
        fileStore.map(EditorUtil.getEditorInput) foreach { input =>
          withDocument(input) { document =>
            {
              // init document model
              val model = new EditDocumentModel(isabelleModel.session, document, node)
              model.init()
              // dispose immediately after initialisation
              model.dispose()
            }
          }
        }

      })
    }

    private def pendingDependencies(): List[Document.Node.Name] = {

      val thyInfo = new Thy_Info(isabelleModel.session.thy_load)

      val currentName = isabelleModel.name

      // get the dependencies for this name and filter the duplicates as well as this editor
      val dependencies = thyInfo.dependencies(true, List(currentName)).deps
      val dependencyNodes = dependencies.map(_.name).distinct.filter(_ != currentName)
      
      // get document models for each open editor and resolve their names
      val loadedNodes = EditorUtil.getOpenEditors.asScala.map(
        editor => adapt(editor.getAdapter _)(classOf[DocumentModel])).flatten.map(_.name).toSet

      dependencyNodes.filterNot(loadedNodes.contains)
    }

    private def withDocument(input: AnyRef)(f: IDocument => Unit) =
      try {
        val provider = getDocumentProvider

        provider.connect(input)
        val document = provider.getDocument(input)
        f(document)
        provider.disconnect(input)

      } catch {
        case e: CoreException => log(error(Some(e)))
      }
  }

}
