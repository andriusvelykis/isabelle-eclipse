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

  def getHyperlink(snapshot: Document.Snapshot, offset: Int): HyperlinkInfo =
  {
      val markup =
        snapshot.select_markup(Text.Range(offset, offset + 1)) {
          // FIXME Isar_Document.Hyperlink extractor
          case Text.Info(info_range,
            XML.Elem(Markup(Markup.ENTITY, props), _)) if (props.find(
            {
              case (Markup.KIND, Markup.ML_OPEN) => true
              case (Markup.KIND, Markup.ML_STRUCT) => true
              case _ => false
            }).isEmpty) =>
            
            (Position.File.unapply(props), Position.Line.unapply(props)) match {
              case (Some(def_file), Some(def_line)) => {
                // get start/end offsets if available
                
                val def_range = (Position.Offset.unapply(props), Position.End_Offset.unapply(props)) match {
                  // workaround - the offset is off by 1 somehow
                  case (Some(def_start), Some(def_end)) => Text.Range(def_start - 1, def_end - 1)
                  case _ => null
                }
                val def_name = Markup.Name.unapply(props) match {
                  case Some(name) => name
                  case None => null
                }
                
                // do not have name positions - so just use the whole def_start/end
                HyperlinkInfo(info_range, null, def_file, def_name, def_range, def_line, def_range)
              }
              case _ if !snapshot.is_outdated =>
                (props, props) match {
                  case (Position.Id(def_id), Position.Offset(def_offset)) =>
                    snapshot.state.find_command(snapshot.version, def_id) match {
                      case Some((def_node, def_cmd)) =>
                        def_node.command_start(def_cmd) match {
                          case Some(def_cmd_start) => {
                            
                            val def_range = Text.Range(def_cmd_start, def_cmd_start + def_cmd.length)
                            
                            val def_name_range = Position.End_Offset.unapply(props) match {
                              case Some(end) => Text.Range(def_cmd_start + def_cmd.decode(def_offset),
                                                           def_cmd_start + def_cmd.decode(end))
                              case None => null
                            }
                        	
                            val def_name = Markup.Name.unapply(props) match {
                              case Some(name) => name
                              case None => null
                            }
                            HyperlinkInfo(info_range, new DocumentRef(def_cmd.node_name), 
                                def_cmd.node_name.node, def_name, def_range, -1, def_name_range)
                          }
                          case None => null
                        }
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

  sealed case class HyperlinkInfo(val linkRange: Text.Range, 
      val targetDocument: DocumentRef, val targetFile: String, val targetName: String,
      val targetRange: Text.Range, val targetLine: Int, val targetNameRange: Text.Range)
  
}