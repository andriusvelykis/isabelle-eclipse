
package isabelle.scala

import java.util.ArrayList

import scala.collection.JavaConversions._
import scala.collection.mutable

import isabelle._
import Thy_Syntax.Structure

object TheoryNode {

  def getTree(session: Session, thy_name: Document.Node.Name, text: String): java.util.List[TheoryNode] = {
    val syntax = session.current_syntax()

    def make_tree(offset: Text.Offset, entry: Structure.Entry): List[TheoryNode] =
      entry match {
        case Structure.Block(name, body) =>
          val node = new TheoryNode(Library.first_line(name), offset, offset + entry.length)
          (offset /: body)((i, e) =>
            {
              make_tree(i, e) foreach (nd => node.add(nd))
              i + e.length
            })
          List(node)
        case Structure.Atom(command)
        if command.is_command && syntax.heading_level(command).isEmpty =>
          val node = new TheoryNode(command.name, offset, offset + entry.length)
          List(node)
        case _ => Nil
      }
    
    val structure = Structure.parse(syntax, thy_name, text)

    make_tree(0, structure)
  }

  def getRawTree(snapshot : Document.Snapshot) : TheoryNode = {

    val root = new TheoryNode("root",0,0)
    
    for ((command, command_start) <- snapshot.node.command_range()) {
      node_tree(snapshot.command_state(command).root_markup, root)((info: Text.Info[Any]) =>
        {
          val range = info.range + command_start
          val content = command.source(info.range).replace('\n', ' ')
          val info_text =
            info.info match {
              case elem @ XML.Elem(_, _) =>
                Pretty.formatted(List(elem), margin = 40).mkString("\n")
              case x => x.toString
            }

          new TheoryNode(command.toString, range.start, range.stop, content, info_text)
        })
    }
    root
  }

  // adapted from swing_tree
  private def node_tree(tree: Markup_Tree, parent: TheoryNode)(nodeMap: Text.Info[Any] => TheoryNode)
  {
    for ((_, (info, subtree)) <- tree.branches) {
      val current = nodeMap(info)
      node_tree(subtree, current)(nodeMap)
      parent.add(current)
    }
  }

}

class TheoryNode(val name: String, val start: Text.Offset, val end: Text.Offset,
  val content : String, val info_text : String) {

  def this(name: String, start: Text.Offset, end: Text.Offset) = this(name, start, end, name, name)

  private val children = new ArrayList[TheoryNode]
  private var parent : TheoryNode = null

  def getParent() : TheoryNode = parent

  def add(child : TheoryNode) {
    children.add(child)
    child.parent = this
  }

  def getChildren() : java.util.List[TheoryNode] = children

  override def toString = "\"" + content + "\" [" + start + ":" + end + "]"

}
