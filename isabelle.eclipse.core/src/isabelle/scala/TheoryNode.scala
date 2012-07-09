
package isabelle.scala

import java.util.ArrayList
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode

import scala.collection.JavaConversions.seqAsJavaList

import isabelle.Document
import isabelle.Library
import isabelle.Pretty
import isabelle.Session
import isabelle.Text
import isabelle.Thy_Syntax.Structure
import isabelle.XML


object TheoryNode {

  def getTree(session: Session, thy_name: DocumentRef, text: String): java.util.List[TheoryNode] = {
    val syntax = session.recent_syntax()

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
    
    val structure = Structure.parse(syntax, thy_name.getRef(), text)

    make_tree(0, structure)
  }

  def getRawTree(snapshot : Document.Snapshot) : TheoryNode = {

    val root = new DefaultMutableTreeNode(new TheoryNode("root",0,0))
    
    for ((command, command_start) <- snapshot.node.command_range()) {
      snapshot.state.command_state(snapshot.version, command).markup
        .swing_tree(root)((info: Text.Info[List[XML.Elem]]) =>
          {
            val range = info.range + command_start
            val content = command.source(info.range).replace('\n', ' ')
            val info_text =
              Pretty.formatted(Library.separate(Pretty.FBreak, info.info), margin = 40).mkString

            new DefaultMutableTreeNode(new TheoryNode(command.toString, range.start, range.stop, content, info_text))
          })
    }
    
    adaptTreeNode(root)
  }
  
  // convert from Swing tree node to custom tree data structure
  // (maybe get rid of Swing here eventually - need to adapt the API)
  private def adaptTreeNode(node: DefaultMutableTreeNode): TheoryNode = {
    val thNode = node.getUserObject.asInstanceOf[TheoryNode]
    
    // recursively adapt each child and add to the theory node
    nodeChildren(node).map(adaptTreeNode).foreach(thNode.add)
    
    thNode
  }
  
  private def nodeChildren[N <: TreeNode](node: N) =
    for (i <- 0 to node.getChildCount) yield node.getChildAt(i).asInstanceOf[N]

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
