package isabelle.eclipse.ui.views

import scala.actors.Actor._

import org.eclipse.jface.layout.TreeColumnLayout
import org.eclipse.jface.resource.JFaceResources
import org.eclipse.jface.viewers.{
  AbstractTreeViewer,
  ColumnViewerToolTipSupport,
  ColumnWeightData,
  ITreeContentProvider,
  StyledCellLabelProvider,
  TreeViewerColumn,
  Viewer,
  ViewerCell
}
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyleRange
import org.eclipse.swt.graphics.{Font, Image}
import org.eclipse.swt.widgets.Composite
import org.eclipse.ui.{ISharedImages, PlatformUI}
import org.eclipse.ui.dialogs.{FilteredTree, PatternFilter}
import org.eclipse.ui.part.ViewPart

import isabelle.Symbol
import isabelle.eclipse.core.IsabelleCore
import isabelle.eclipse.core.app.Isabelle
import isabelle.eclipse.core.util.LoggingActor
import isabelle.eclipse.ui.preferences.IsabelleUIPreferences
import isabelle.eclipse.ui.util.{EclipseBug154341Hack, SWTUtil}


/**
 * A view that lists all available Isabelle symbols.
 *
 * Supports search and uses a tree view to display symbol groups.
 *
 * @author Andrius Velykis
 */
class IsabelleSymbolsView extends ViewPart {
  
  /** a listener for system init event  */
  private val systemListener = LoggingActor {
    loop {
      react {
        case Isabelle.SystemInit =>
          SWTUtil.asyncExec(Some(viewer.getDisplay)) { initSymbols() }
        case _ =>
      }
    }
  }
  
  private var viewer: SymbolFilteredTree = _
  
  override def createPartControl(parent: Composite) {
    
    viewer = new SymbolFilteredTree(parent, SWT.SINGLE | SWT.FULL_SELECTION)
    
    // add listener to the isabelle app to react to session init
    val isabelle = IsabelleCore.isabelle
    isabelle.systemEvents += systemListener
    
    if (isabelle.isInit) {
      initSymbols()
    }
  }
  
  override def dispose() {
    IsabelleCore.isabelle.systemEvents -= systemListener
  }

  override def setFocus() = viewer.setFocus
  
  private def initSymbols() {
    val input = symbolsGroups
    viewer.getViewer.setInput(input)
  }

  private def symbolsGroups: List[SymbolGroup] =
    Symbol.groups map { case (name, symbols) => SymbolGroup(name, symbols) }
  
  // TODO preference change
  private def isabelleFont: Font =
    JFaceResources.getFontRegistry.get(IsabelleUIPreferences.ISABELLE_FONT)


  private case class SymbolGroup(name: String, symbols: List[Symbol.Symbol])


  /**
   * A customised FilteredTree with Symbol renderers and content providers
   */
  private class SymbolFilteredTree(parent: Composite, treeStyle: Int)
      extends FilteredTree(parent, treeStyle, new SymbolPatternFilter, true) {

    getViewer.setContentProvider(new SymbolTreeContentProvider)
    val defaultRowHeight = getViewer.getTree.getItemHeight
    updateRowHeight()

    val treeLayout = new TreeColumnLayout
    treeComposite.setLayout(treeLayout)
    
    val symbolColumn = {
      val col = new TreeViewerColumn(treeViewer, SWT.CENTER)
      col.getColumn.setAlignment(SWT.CENTER)
      col.setLabelProvider(new SymbolLabelProvider)
      treeLayout.setColumnData(col.getColumn, new ColumnWeightData(100, false))
      col
    }
    
    // expand the groups
    getViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS)
    
    // enable tooltips
    ColumnViewerToolTipSupport.enableFor(getViewer)


    private def updateRowHeight() {
      val fontMetrics = SWTUtil.initializeFontMetrics(getViewer.getTree, isabelleFont)
      val fontRowHeight = fontMetrics.getHeight// + 2
      
      val rowHeight = fontRowHeight max defaultRowHeight
      
      EclipseBug154341Hack.setItemHeight(getViewer, rowHeight)
    }
  }


  private class SymbolLabelProvider extends StyledCellLabelProvider {

    lazy val font = isabelleFont
    lazy val groupImage = PlatformUI.getWorkbench.getSharedImages.getImage(
      ISharedImages.IMG_OBJ_FOLDER)


    def styledText(element: Any): (String, List[StyleRange]) = element match {

      case s: Symbol.Symbol => {
        val decoded = Symbol.decode(s)
        val name = (Symbol.names.get(s) map ("  " + _)) getOrElse ""

        val symLength = decoded.length
        val symStyle = new StyleRange(0, symLength, null, null)
        symStyle.font = font

        (decoded + name, List(symStyle))
      }

      case SymbolGroup(name, _) => {
        val prettyName = name.split("\\_").map(_.capitalize).mkString(" ")
        (prettyName, Nil)
      }

      case e => (String.valueOf(e), Nil)
    }
    
    def image(element: Any): Image = element match {
      case SymbolGroup(_, _) => groupImage
      case _ => null
    }

    override def update(cell: ViewerCell) {

      val element = cell.getElement
      val (text, style) = styledText(element)
      val img = image(element)

      cell.setText(text)
      cell.setStyleRanges(style.toArray)
      cell.setImage(img)

      super.update(cell)
    }

    override def getToolTipText(element: Any): String = element match {
      case s: Symbol.Symbol =>
        s + "\n" +
          (Symbol.abbrevs.get(s) map ("abbrev: " + _ + "\n")).getOrElse("") +
          "\n(Double-click to insert)"

      case _ => null
    }
  }


  private class SymbolTreeContentProvider extends ITreeContentProvider {

    override def getElements(parentElement: AnyRef): Array[AnyRef] = parentElement match {
      case list: TraversableOnce[_] => list.asInstanceOf[TraversableOnce[AnyRef]].toArray
      case _ => getChildren(parentElement)
    }

    override def getChildren(parentElement: AnyRef): Array[AnyRef] = parentElement match {
      case SymbolGroup(_, symbols) => symbols.toArray
      case _ => Array()
    }

    override def getParent(element: AnyRef): AnyRef = null

    override def hasChildren(element: AnyRef): Boolean = !getChildren(element).isEmpty

    override def inputChanged(viewer: Viewer, oldInput: AnyRef, newInput: AnyRef) {}

    override def dispose() {}
  }


  /**
   * A special symbol filter that checks various symbol representations when searching
   */
  private class SymbolPatternFilter extends PatternFilter {

    override def isLeafMatch(viewer: Viewer, element: Any): Boolean = element match {

      case SymbolGroup(name, _) => wordMatches(name)

      // check symbol, its contents and abbreviation to allow for various searches
      case s: Symbol.Symbol => 
        wordMatches(Symbol.decode(s)) ||
        wordMatchesValue(Symbol.names, s) ||
        wordMatches(s) ||
        wordMatchesValue(Symbol.abbrevs, s)

      case e => super.isLeafMatch(viewer, e)

    }

    private def wordMatchesValue(map: Map[Symbol.Symbol, String], s: Symbol.Symbol): Boolean =
      map.get(s) exists wordMatches
  }
  
}
