package isabelle.eclipse.ui.text.hyperlink

import org.eclipse.jface.text.{IRegion, ITextViewer, Region}
import org.eclipse.jface.text.hyperlink.{AbstractHyperlinkDetector, IHyperlink, IHyperlinkDetectorExtension2}
import org.eclipse.swt.SWT

import isabelle.{Markup, Text, XML}
import isabelle.Document.Snapshot
import isabelle.eclipse.ui.editors.{EditorUtil, TheoryEditor}


/**
 * A hyperlink detector for Isabelle Action markup, e.g. "sendback".
 * 
 * Simulates real hyperlinks by not requiring some key masks to activate detector.
 *
 * @author Andrius Velykis
 */
class IsabelleActionHyperlinkDetector(snapshot: => Option[Snapshot],
                                      targetEditor: => Option[TheoryEditor])
    extends AbstractHyperlinkDetector with IHyperlinkDetectorExtension2 {

  /**
   * No state mask - this will simulate real links, so that they can be clickable without any
   * mask hover.
   */
  override def getStateMask(): Int = SWT.NONE

  override def detectHyperlinks(textViewer: ITextViewer,
                                region: IRegion,
                                canShowMultipleHyperlinks: Boolean): Array[IHyperlink] =
    (snapshot map { s =>

      val links = hyperlinks(s, new Text.Range(region.getOffset, region.getOffset + 1))

      // cannot return empty array - use null instead
      if (links.isEmpty) null
      else if (!canShowMultipleHyperlinks) Array(links.head)
      else links.toArray
    }).orNull


  private val activeInclude =
    Set(Markup.BROWSER, Markup.GRAPHVIEW, Markup.SENDBACK, Markup.DIALOG)

  private def hyperlinks(snapshot: Snapshot, range: Text.Range): Stream[IHyperlink] = {
    
    def linkRegion(range: Text.Range) = toRegion(snapshot.convert(range))

    val hyperlinkInfos =
      // adapted from isabelle.jedit.Rendering#active and isabelle.jedit.Active
      snapshot.select_markup(range, Some(activeInclude), command_state =>
        {
          // TODO implement dialog and other active links below
//          case Text.Info(info_range, elem @ Protocol.Dialog(id, serial, result)) 
//            if !command_state.results.defined(serial) =>
//              Text.Info(snapshot.convert(info_range), elem)

          case Text.Info(info_range, XML.Elem(Markup(Markup.SENDBACK, props), _)) =>
            new SendbackHyperlink(linkRegion(info_range),
              targetEditor flatMap (_.isabelleModel),
              targetEditor flatMap (ed => Option(EditorUtil.getTextViewer(ed))),
              props,
              snapshotText(snapshot, snapshot.convert(info_range)))

          case Text.Info(info_range, XML.Elem(Markup(Markup.BROWSER, _), body)) =>
            new TheoryGraphHyperlink(linkRegion(info_range), body)

//          case Text.Info(info_range, elem @ XML.Elem(Markup(name, _), _))
//            if name == Markup.BROWSER || name == Markup.GRAPHVIEW || name == Markup.SENDBACK =>
//              Text.Info(snapshot.convert(info_range), elem)
        })
    
    hyperlinkInfos map { case Text.Info(_, hyperlink) => hyperlink }
  }
  
  
  private def snapshotText(snapshot: Snapshot, range: Text.Range): String = {
    val cmds = snapshot.node.command_range(range).toList
    
    if (cmds.isEmpty) {
      ""
    } else {
      val cmdsText = (cmds map (_._1.source)).mkString  
      val cmdsOffset = cmds.head._2
      val rangeInCmd = range - cmdsOffset
      cmdsText.substring(rangeInCmd.start, rangeInCmd.stop)
    }
  }


  private def toRegion(range: Text.Range): IRegion =
    new Region(range.start, range.length)

}
