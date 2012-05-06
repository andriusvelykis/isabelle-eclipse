package isabelle.scala

import isabelle._

/**
 * Isabelle hyperlink calculation from the snapshot markup data.
 * <p>
 * Adapted from isabelle.jedit.Isabelle_Hyperlinks (Isabelle/jEdit)
 * </p>
 * 
 * @author Andrius Velykis
 */
object HyperlinkUtil {

  def getHyperlink(snapshot: Document.Snapshot, offset: Int, length: Int = 1): HyperlinkInfo =
  {
      val markup =
        snapshot.select_markup(Text.Range(offset, offset + length)) {
          // FIXME Isar_Document.Hyperlink extractor
          case Text.Info(info_range,
            XML.Elem(Markup(Markup.ENTITY, props), _)) if (props.find(
            {
              case (Markup.KIND, Markup.ML_OPEN) => true
              case (Markup.KIND, Markup.ML_STRUCT) => true
              case _ => false
            }).isEmpty) =>
            
            (Position.File.unapply(props), Position.Offset.unapply(props), Position.End_Offset.unapply(props)) match {
              case (Some(def_file), Some(def_start), Some(def_end)) => {
                // workaround - the offset is off by 1 somehow
                val def_range = Text.Range(def_start - 1, def_end - 1)
                TextHyperlinkInfo(info_range, Markup.Name.unapply(props).orNull, def_file, def_range)
              }
              case _ if !snapshot.is_outdated =>
                (props, props, props) match {
                  case (Position.Id(def_id), Position.Offset(def_offset), Position.End_Offset(end_offset)) =>
                    snapshot.state.find_command(snapshot.version, def_id) match {
                      case Some((def_node, def_cmd)) =>
                        CommandHyperlinkInfo(info_range, Markup.Name.unapply(props).orNull, def_cmd,
                            Text.Range(def_offset, end_offset))
                      case None => null
                    }
                  case _ => null
                }
              case _ => null
            }
        }
      markup match {
        case Text.Info(_, Some(link)) #:: _ => link
        case _ => null
      }
  }
  
  trait HyperlinkInfo {
    def linkRange: Text.Range
    def linkName: String
  }

  sealed case class CommandHyperlinkInfo(val linkRange: Text.Range, val linkName: String, 
      val targetCommand: Command, val rangeInCommand: Text.Range) extends HyperlinkInfo
  
  sealed case class TextHyperlinkInfo(val linkRange: Text.Range, val linkName: String,
      val targetFile: String, val targetRange: Text.Range) extends HyperlinkInfo
  
}