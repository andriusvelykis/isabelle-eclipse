package isabelle.eclipse.ui.editors

import java.net.URI

import org.eclipse.jface.text.{IRegion, ITextViewer, Region}
import org.eclipse.jface.text.hyperlink.{AbstractHyperlinkDetector, IHyperlink}
import org.eclipse.ui.{IWorkbenchPage, PlatformUI}

import isabelle.{Command, Isabelle_System, Markup, Path, Position, Text, XML}
import isabelle.Document.Snapshot
import isabelle.eclipse.core.resource.URIThyLoad
import isabelle.eclipse.core.resource.URIThyLoad.toURINodeName


/**
 * A hyperlink detector for Isabelle markup snapshot.
 * 
 * @author Andrius Velykis
 */
class IsabelleHyperlinkDetector(
  snapshot: => Option[Snapshot],
  workbenchPage: => Option[IWorkbenchPage] = 
    Option(PlatformUI.getWorkbench.getActiveWorkbenchWindow.getActivePage))
    extends AbstractHyperlinkDetector {

  override def detectHyperlinks(textViewer: ITextViewer,
                                region: IRegion,
                                canShowMultipleHyperlinks: Boolean): Array[IHyperlink] =
    (snapshot, workbenchPage) match {
      case (Some(s), Some(page)) => {

        val links = hyperlinks(s, new Text.Range(region.getOffset, region.getOffset + 1))

        val viewerLinks = links map {
          case Text.Info(linkRange, linkInfo) => viewerHyperlink(page, linkRange, linkInfo)
        }

        // cannot return empty array - use null instead
        if (viewerLinks.isEmpty) null
        else if (!canShowMultipleHyperlinks) Array(viewerLinks.head)
        else viewerLinks.toArray
      }
      case _ => null
    }


  private val hyperlinkInclude = Set(Markup.ENTITY, Markup.PATH)

  private def hyperlinks(snapshot: Snapshot, range: Text.Range): Stream[Text.Info[Hyperlink]] =
  {
    val hyperlinkLists =
      snapshot.cumulate_markup[List[Text.Info[Hyperlink]]](range, Nil, Some(hyperlinkInclude), _ =>
        {
          case (links, Text.Info(info_range, XML.Elem(Markup.Path(name), _)))
          if Path.is_ok(name) =>
            val targetUri = URIThyLoad.resolveURI(snapshot.node_name.uri, Path.explode(name))
            Text.Info(snapshot.convert(info_range), Hyperlink(targetUri, 0, 0, Some(name))) :: links

          case (links, Text.Info(info_range, XML.Elem(Markup(Markup.ENTITY, props), _)))
          if !props.exists(
            { case (Markup.KIND, Markup.ML_OPEN) => true
              case (Markup.KIND, Markup.ML_STRUCT) => true
              case _ => false }) =>

            props match {
              case DefFileOffsetEnd(name, offset, end) if Path.is_ok(name) =>
                Isabelle_System.source_file(Path.explode(name)) match {
                  case Some(path) =>
                    val fileUri = URIThyLoad.isabellePathUri(path)
                    Text.Info(snapshot.convert(info_range),
                      // workaround - the offsets are off by 1 somehow
                      Hyperlink(fileUri, offset-1 max 0, end-1 max 0, Markup.Name.unapply(props))) :: links
                  case None => links
                }

              case DefIdOffsetEnd(id, offset, end) =>
                snapshot.state.find_command(snapshot.version, id) match {
                  case Some((node, command)) => {
                    val rangeInCmd = new Text.Range(command.decode(offset), command.decode(end))
                    Text.Info(snapshot.convert(info_range),
                      Hyperlink(command, rangeInCmd, Markup.Name.unapply(props))) :: links
                  }
                  case None => links
                }

              case _ => links
            }
        })// match { case Text.Info(_, info :: _) #:: _ => Some(info) case _ => None }
    
    (hyperlinkLists map { case Text.Info(_, hyperlinkInfos) => hyperlinkInfos }).flatten
  }

  private def viewerHyperlink(page: IWorkbenchPage,
                              range: Text.Range,
                              info: Hyperlink): IHyperlink = {

    val linkRegion = toRegion(range)

    info match {
      case URILink(uri, offset, end, name) => {
        // create text hyperlink with calculated information about target URI/location
        val linkRegion = toRegion(range)
        val targetRegion = toRegion(new Text.Range(offset, end))
        // the relative path may be a workspace file (platform: URI), so resolve it to file URI
        val resolvedUri = URIThyLoad.resolvePlatformUri(uri)
        new TextHyperlink(page, linkRegion, name, resolvedUri, targetRegion, targetRegion)
      }
      case CommandLink(command, rangeInCmd, name) => {
        // create command hyperlink, which determines the target editor/location from the command
        new IsabelleCommandHyperlink(page, linkRegion, name, command, Some(toRegion(rangeInCmd)))
      }
    }
  }
  
  private def toRegion(range: Text.Range): IRegion =
    new Region(range.start, range.length)

  /**
   * Extractor object for command definition (in the same theory).
   *
   * Retrieves command ID and definition range within the command.
   */
  object DefIdOffsetEnd {
    import Position._
    def unapply(pos: T): Option[(Long, Text.Offset, Text.Offset)] =
      (pos, pos, pos) match {
        case (Def_Id(id), Def_Offset(offset), Def_End_Offset(end)) => Some((id, offset, end))
        case (Def_Id(id), Def_Offset(offset), _) => Some((id, offset, offset))
        case _ => None
      }
  }

  /**
   * Extractor object for file-based definition (in another file).
   *
   * Retrieves file name and definition range.
   */
  object DefFileOffsetEnd {
    import Position._
    def unapply(pos: T): Option[(String, Text.Offset, Text.Offset)] =
      (pos, pos, pos) match {
        case (Def_File(name), Def_Offset(offset), Def_End_Offset(end)) => Some((name, offset, end))
        case (Def_File(name), Def_Offset(offset), _) => Some((name, offset, offset))
        case (Def_File(name), _, _) => Some((name, 0, 0))
        case _ => None
      }
  }

  private sealed trait Hyperlink
  private case class URILink(val uri: URI,
                             val offset: Text.Offset,
                             val end: Text.Offset,
                             val name: Option[String]) extends Hyperlink

  private case class CommandLink(val command: Command,
                                 val rangeInCmd: Text.Range,
                                 val name: Option[String]) extends Hyperlink

  private object Hyperlink {
    def apply(uri: URI, offset: Text.Offset, end: Text.Offset, name: Option[String]): Hyperlink =
      URILink(uri, offset, end, name)

    def apply(command: Command, rangeInCmd: Text.Range, name: Option[String]): Hyperlink =
      CommandLink(command, rangeInCmd, name)
  }
  
}
