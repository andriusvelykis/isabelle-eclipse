package isabelle.eclipse.ui.text.hover

import scala.util.Try

import org.eclipse.jface.text.{
  DefaultInformationControl,
  DefaultTextHover,
  IDocument,
  IInformationControl,
  IInformationControlCreator,
  IRegion,
  ITextHoverExtension,
  ITextHoverExtension2,
  ITextViewer,
  Region
}
import org.eclipse.jface.text.source.ISourceViewer
import org.eclipse.swt.widgets.Shell
import org.eclipse.ui.editors.text.EditorsUI

import isabelle.{Markup, Path, Pretty, Session, Text, XML}
import isabelle.Document.Snapshot


/**
 * A hover (tooltip) support for Isabelle markup.
 *
 * Shows the information taken from Isabelle markup (e.g. types) in addition to the annotations
 * in the text.
 *
 * @author Andrius Velykis
 */
class IsabelleTextHover(session: => Option[Session],
                        snapshot: => Option[Snapshot],
                        viewer: ISourceViewer,
                        showAnnotationTooltips: Boolean,
                        showIsabelleTooltips: Boolean)
    extends DefaultTextHover(viewer) with ITextHoverExtension with ITextHoverExtension2 {

  override def getHoverRegion(textViewer: ITextViewer, offset: Int): IRegion =
    // avoid calculating twice and just give the empty region at offset here
    new Region(offset, 1)


  override def getHoverInfo(textViewer: ITextViewer, hoverRegion: IRegion): String =
    throw new UnsupportedOperationException("deprecated")


  override def getHoverInfo2(textViewer: ITextViewer, hoverRegion: IRegion): AnyRef = {

    def annotationTooltip = {
      val blank = textAt(textViewer.getDocument, hoverRegion) forall (_.isWhitespace)
      // if hovering over some characters - try getting annotation text
      if (blank) None else Option(super.getHoverInfo(textViewer, hoverRegion))
    }

    def isabelleTooltip = (session, snapshot) match {
      case (Some(session), Some(snapshot)) => Some(tooltip(session, snapshot,
        Text.Range(hoverRegion.getOffset, hoverRegion.getOffset + hoverRegion.getLength)))
      case _ => None
    }
    
    val annTip = if (showAnnotationTooltips) annotationTooltip else None
    val isaTip = if (showIsabelleTooltips) isabelleTooltip else None

    val allTips = (isaTip ++ annTip) filterNot (_.isEmpty)
    
    // use line separator if both are showing
    allTips.mkString(separator)
  }
  
  private def textAt(document: IDocument, region: IRegion): String = {
    val docEnd = document.getLength
    val offset = (region.getOffset max 0) min docEnd
    val length = (region.getLength min (docEnd - offset)) max 0
    
    Try(document.get(offset, length)) getOrElse ""
  }

    
//  val tooltipMessages = Set(Markup.WRITELN, Markup.WARNING, Markup.ERROR, Markup.BAD)

  private val tooltips: Map[String, String] =
    Map(
      Markup.SORT -> "sort",
      Markup.TYP -> "type",
      Markup.TERM -> "term",
      Markup.PROP -> "proposition",
      Markup.TOKEN_RANGE -> "inner syntax token",
      Markup.FREE -> "free variable",
      Markup.SKOLEM -> "skolem variable",
      Markup.BOUND -> "bound variable",
      Markup.VAR -> "schematic variable",
      Markup.TFREE -> "free type variable",
      Markup.TVAR -> "schematic type variable",
      Markup.ML_SOURCE -> "ML source",
      Markup.DOCUMENT_SOURCE -> "document source")

  private val tooltipElements =
    Set(Markup.ENTITY, Markup.SORTING, Markup.TYPING, Markup.ML_TYPING, Markup.PATH) ++
      tooltips.keys// ++ tooltipMessages


  def tooltip(session: Session, snapshot: Snapshot, range: Text.Range): String = {
    
    def prettyTyping(label: String, content: XML.Body): String = 
      // TODO reuse Pretty.formatted somehow?
      label + " " + XML.content(Pretty.formatted(content, 100))//, Pretty_UI.font_metric(fm))

    val tips =
      snapshot.cumulate_markup[List[Text.Info[(Boolean, String)]]](
        range, Nil, Some(tooltipElements), _ =>
        {
          case (prevs, Text.Info(r, XML.Elem(Markup.Entity(kind, name), _))) =>
            val kind1 = (kind split "\\_").mkString(" ")
            val msg = (true, kind1 + " \"" + name + "\"")
            Text.Info(r, msg) :: prevs

          case (prevs, Text.Info(r, XML.Elem(Markup.Path(name), _)))
          if Path.is_ok(name) =>
            // TODO check URI loading here
            val file = session.thy_load.append(snapshot.node_name.dir, Path.explode(name))
            val msg = (true, "file \"" + file + "\"")
            Text.Info(r, msg) :: prevs

          case (prevs, Text.Info(r, XML.Elem(Markup(name, _), body)))
          if name == Markup.SORTING || name == Markup.TYPING =>
            Text.Info(r, (true, prettyTyping("::", body))) :: prevs

          case (prevs, Text.Info(r, XML.Elem(Markup(Markup.ML_TYPING, _), body))) =>
            Text.Info(r, (false, prettyTyping("ML:", body))) :: prevs

          case (prevs, Text.Info(r, XML.Elem(Markup(name, _), _)))
          if tooltips.isDefinedAt(name) =>
            Text.Info(r, (true, tooltips(name))) :: prevs

        }).toList.flatMap(_.info)

    val allTips =
      (tips.filter(_.info._1) ++ tips.filter(!_.info._1).lastOption.toList)
    
    // group tooltips
    val groupedTips = groupByRange(allTips)
    
    // within group, put every tooltip in a newline and then separate groups using separator
    val groupedTipStrs = groupedTips map outputTooltipGroup
    groupedTipStrs.mkString(separator)
  }

  private def separator: String = "\n\n"

  private def groupByRange(tips: List[Text.Info[(Boolean, String)]]):
      List[List[Text.Info[(Boolean, String)]]] = {
    
    // group tooltips by their range (different range goes to different group)
    val groups = tips groupBy (_.range)
    // sort by length (shortest at top)
    val sorted = groups.toList.sortBy(_._1.length)
    
    // get the results
    sorted map (_._2)
  }

  private def outputTooltipGroup(tips: List[Text.Info[(Boolean, String)]]): String = {
    val tipTexts = tips map (_.info._2)
    tipTexts mkString "\n"
  }

  override def getHoverControlCreator: IInformationControlCreator =
    new IInformationControlCreator {
      override def createInformationControl(parent: Shell): IInformationControl = {
        // null presenter to print text as it is
        // TODO use better presenter to show syntax highlighting, markup, etc
        // reuse Isabelle source viewer?
        return new DefaultInformationControl(parent, EditorsUI.getTooltipAffordanceString, null)
      }
    };
  
}
