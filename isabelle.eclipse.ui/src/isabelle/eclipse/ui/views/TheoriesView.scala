package isabelle.eclipse.ui.views

import scala.actors.Actor._

import org.eclipse.jface.layout.{GridDataFactory, GridLayoutFactory, TreeColumnLayout}
import org.eclipse.jface.resource.{JFaceResources, LocalResourceManager, ResourceManager}
import org.eclipse.jface.viewers.{
  AbstractTreeViewer,
  CellLabelProvider,
  ColumnLabelProvider,
  ColumnViewerToolTipSupport,
  ColumnWeightData,
  DoubleClickEvent,
  IDoubleClickListener,
  IStructuredSelection,
  ITreeContentProvider,
  StructuredViewer,
  TreeViewerColumn,
  Viewer,
  ViewerCell
}
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.TreeEditor
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.widgets.{Composite, Label, ProgressBar, TreeItem}
import org.eclipse.ui.{ISharedImages, PartInitException, PlatformUI}
import org.eclipse.ui.dialogs.{FilteredTree, PatternFilter}
import org.eclipse.ui.ide.IDE
import org.eclipse.ui.part.ViewPart

import isabelle.{Document, Protocol, Session}
import isabelle.eclipse.core.IsabelleCore
import isabelle.eclipse.core.resource.URIThyLoad
import isabelle.eclipse.core.util.{LoggingActor, SessionEvents}
import isabelle.eclipse.ui.editors.TheoryEditor
import isabelle.eclipse.ui.internal.IsabelleImages
import isabelle.eclipse.ui.internal.IsabelleUIPlugin.{error, log}
import isabelle.eclipse.ui.util.SWTUtil


/**
 * A view that lists all open Isabelle theories.
 *
 * The theories are accompanied with a progress bar that provides an overview of their status
 * in the prover. Furthermore, an icon provides an overall status of the theory
 * (e.g. error, warning, finished or unfinished).
 *
 * @author Andrius Velykis
 */
class TheoriesView extends ViewPart with SessionEvents {

  // the actor to react to session events
  override protected val sessionActor = LoggingActor {
    loop {
      react {
        case phase: Session.Phase => runInUI { handlePhase(Some(phase)) }

        case changed: Session.Commands_Changed => runInUI { handleUpdate(Some(changed.nodes)) }

        case bad => System.err.println("Theories view: ignoring bad message " + bad)
      }
    }
  }

  // subscribe to commands change session events
  override protected def sessionEvents(session: Session) =
    List(session.phase_changed, session.commands_changed)

  // TODO this is a bit too late for session phase - need to listen WHILE it's starting as well
  override def sessionInit(session: Session) = runInUI {
    handlePhase(Some(session.phase))
  } 

  private var nodeStatus: Map[Document.Node.Name, Protocol.Node_Status] = Map()
  private var nodeProgress: Map[Document.Node.Name, (TreeEditor, ProgressBar)] = Map()

  private var phaseLabel: Label = _
  private var viewer: TheoriesFilteredTree = _

  private val resourceManager: ResourceManager =
    new LocalResourceManager(JFaceResources.getResources)
  
  override def createPartControl(parent: Composite) {
    
    val main = new Composite(parent, SWT.NONE)
    main.setLayout(GridLayoutFactory.fillDefaults.create)

    phaseLabel = new Label(main, SWT.WRAP)
    phaseLabel.setLayoutData(GridDataFactory.swtDefaults.create)

    val separator = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL)
    separator.setLayoutData(GridDataFactory.fillDefaults.grab(true, false).create)

    viewer = new TheoriesFilteredTree(main, SWT.SINGLE | SWT.FULL_SELECTION)
    viewer.setLayoutData(GridDataFactory.fillDefaults.grab(true, true).create)
    viewer.getViewer.setInput(List())
    
    initSessionEvents()
    
    // show current prover phase
    handlePhase(IsabelleCore.isabelle.session map (_.phase))

    // show current theories
    handleUpdate()
  }
  
  override def dispose() {
    disposeSessionEvents()
    resourceManager.dispose()
  }

  override def setFocus() = viewer.setFocus

  private def handlePhase(phase: Option[Session.Phase]) {
    val phaseStr = phase map (_.toString.toLowerCase) getOrElse "not started"
    phaseLabel.setText("Prover: " + phaseStr)
  }

  private def handleUpdate(restriction: Option[Set[Document.Node.Name]] = None) {
    
    val session = IsabelleCore.isabelle.session
    val updates = session map (s => updateNodeStatus(s, restriction))

    updates match {
      case Some((Some(updatedThyList), changedNodes)) => {
        val currentThyList = viewer.getViewer.getInput
        if (updatedThyList == currentThyList) {
          // just refresh the changed nodes
          if (!updatedThyList.isEmpty) {
            // jumping through hoops due to method overload..
            val v: StructuredViewer = viewer.getViewer
            val changed: Array[Object] = changedNodes.toArray
            v.update(changed, null)
          }
        } else {
          // set the new list
          viewer.getViewer.setInput(updatedThyList)
        }
      }

      case _ => // ignore update
    }
  }

  private def updateNodeStatus(session: Session,
                               restriction: Option[Set[Document.Node.Name]]): 
      (Option[List[Document.Node.Name]], List[Document.Node.Name]) = {
    val snapshot = session.snapshot()

    val iterator = restriction match {
      case Some(names) => names.iterator.map(name => (name, snapshot.version.nodes(name)))
      case None => snapshot.version.nodes.entries
    }

    val changedNodeInfo = iterator.toList
    val changedNodes = changedNodeInfo map (_._1)

    val updatedNodeStatus = (changedNodeInfo foldLeft nodeStatus) {
      case (status, (name, node)) =>
        if (session.thy_load.loaded_theories(name.theory)) status
        else status + (name -> Protocol.node_status(snapshot.state, snapshot.version, node))
    }

    if (nodeStatus != updatedNodeStatus) {
      nodeStatus = updatedNodeStatus

      val thysList = snapshot.version.nodes.topological_order.filter(
        (name: Document.Node.Name) => nodeStatus.isDefinedAt(name))
      (Some(thysList), changedNodes)
    } else {
      (None, changedNodes)
    }
  }


  /**
   * On node action (double-click), open the selected document node in the editor.
   */
  private def openTheoryDocument(doc: Document.Node.Name) = {

    // resolve target file URI from the document
    val targetUri = URIThyLoad.resolveDocumentUri(doc)

    try {
      IDE.openEditor(getSite.getPage, targetUri, TheoryEditor.EDITOR_ID, true)
    } catch {
      case e: PartInitException => log(error(Some(e)))
    }
  }

  private def runInUI(f: => Unit) =
    SWTUtil.asyncUnlessDisposed(Option(phaseLabel)) { f }
  

  /**
   * A customised FilteredTree with Theory renderers and content providers
   */
  private class TheoriesFilteredTree(parent: Composite, treeStyle: Int)
      extends FilteredTree(parent, treeStyle, new TheoryPatternFilter, true) {

    getViewer.setContentProvider(new TheoriesTreeContentProvider)

    val treeLayout = new TreeColumnLayout
    treeComposite.setLayout(treeLayout)
    
    val theoryColumn = {
      val col = new TreeViewerColumn(treeViewer, SWT.LEFT)
      col.getColumn.setAlignment(SWT.LEFT)
      col.setLabelProvider(new TheoriesLabelProvider)
      treeLayout.setColumnData(col.getColumn, new ColumnWeightData(60, false))
      col
    }

    val progressColumn = {
      val col = new TreeViewerColumn(treeViewer, SWT.LEFT)
      col.getColumn.setAlignment(SWT.LEFT)
      col.setLabelProvider(new TheoriesProgressLabelProvider)
      treeLayout.setColumnData(col.getColumn, new ColumnWeightData(40, false))
      col
    }

    // expand everything
    getViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS)
    
    // enable tooltips
    ColumnViewerToolTipSupport.enableFor(getViewer)

    // on double-click, replace in editor
    getViewer.addDoubleClickListener(new IDoubleClickListener {
      override def doubleClick(event: DoubleClickEvent) = event.getSelection match {
        case ss: IStructuredSelection => ss.getFirstElement match {
          case doc: Document.Node.Name => openTheoryDocument(doc)
          case _ =>
        }
        case _ =>
      } 
    })

    def tree = getViewer.getTree
  }

  private def tooltip(element: Any): String = element match {

    case doc @ Document.Node.Name(_, _, theory) => {

      val thyStatusMsg = nodeStatus.get(doc) match {
        case Some(status) =>
          "\n---" +
            "\nUnprocessed statements: " + status.unprocessed +
            "\nFinished statements: " + status.finished +
            "\nWarnings: " + status.warned +
            "\nErrors: " + status.failed

        case _ => ""
      }

      "Theory: " + theory + thyStatusMsg + "\n\n(Double-click to open)"
    }

    case _ => null
  }

  private class TheoriesLabelProvider extends ColumnLabelProvider {

    def sharedImages = PlatformUI.getWorkbench.getSharedImages
    lazy val error = sharedImages.getImage(ISharedImages.IMG_OBJS_ERROR_TSK)
    lazy val warning = sharedImages.getImage(ISharedImages.IMG_OBJS_WARN_TSK)
    lazy val finished = resourceManager.createImageWithDefault(IsabelleImages.SUCCESS)
    lazy val unfinished = resourceManager.createImageWithDefault(IsabelleImages.PROGRESS)

    override def getText(element: Any): String = element match {

      case Document.Node.Name(_, _, theory) => theory

      case _ => super.getText(element)
    }

    override def getImage(element: Any): Image = element match {

      case doc: Document.Node.Name => nodeStatus.get(doc) match {

        case Some(status) => if (status.failed > 0) error
        else if (status.warned > 0) warning
        else if (status.unprocessed > 0) unfinished
        else finished

        case unknown => unfinished
      }
      case _ => null
    }

    override def getToolTipText(element: Any): String = tooltip(element)
  }

  /**
   * A label provider that shows progress bar in the cell.
   */
  private class TheoriesProgressLabelProvider extends CellLabelProvider {

    private val max = 100

    override def update(cell: ViewerCell) = cell.getElement match {

      case doc: Document.Node.Name => {

        val (editor, progress) = nodeProgress.get(doc) match {
          case Some(ed) => ed
          case _ => {
            val tree = viewer.getViewer.getTree
            val progress = new ProgressBar(viewer.getViewer.getTree, SWT.NONE)
            progress.setMinimum(0)
            progress.setMaximum(max)
            progress.setSelection(0)
            val editor = new TreeEditor(tree)
            editor.grabHorizontal = true
            editor.grabVertical = true
            nodeProgress += (doc -> (editor, progress))
            (editor, progress)
          }
        }

        val status = nodeStatus.get(doc)
        val selection = status match {
          case Some(status) => {
            val total = status.total
            val processed = total - status.unprocessed

            val ratio = (processed.toDouble / total) * max
            ratio.ceil.toInt
          }
          case None => 0
        }
        progress.setSelection(selection)
        progress.setToolTipText(tooltip(cell.getElement))

        val cellItem = cell.getItem.asInstanceOf[TreeItem]
        if (editor.getItem != cellItem) {
          editor.setEditor(progress, cellItem, 1)
        }
      }
    }
  }


  private class TheoriesTreeContentProvider extends ITreeContentProvider {

    override def getElements(parentElement: AnyRef): Array[AnyRef] = parentElement match {
      case list: TraversableOnce[_] => list.asInstanceOf[TraversableOnce[AnyRef]].toArray
      case _ => getChildren(parentElement)
    }

    override def getChildren(parentElement: AnyRef): Array[AnyRef] = Array()

    override def getParent(element: AnyRef): AnyRef = null

    override def hasChildren(element: AnyRef): Boolean = false

    override def inputChanged(viewer: Viewer, oldInput: AnyRef, newInput: AnyRef) {}

    override def dispose() {}
  }


  private class TheoryPatternFilter extends PatternFilter {

    override def isLeafMatch(viewer: Viewer, element: Any): Boolean = element match {

      case Document.Node.Name(_, _, theory) => wordMatches(theory)

      case e => super.isLeafMatch(viewer, e)
    }
  }
  
}
