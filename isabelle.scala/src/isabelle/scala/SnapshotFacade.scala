
package isabelle.scala

import java.{ util => ju }
import scala.collection.JavaConversions._

import isabelle._

class SnapshotFacade(snapshot : Document.Snapshot) {

  def getSnapshot() = snapshot

  def commandRange(range : Text.Range) : ju.Iterator[CommandInfo] = {
    val toInfo = (elem : (Command, Int)) => new CommandInfo(elem._1, elem._2)
    snapshot.node.command_range(range).map(toInfo)
  }

  class CommandInfo(command : Command, commandStart : Int) {
    def getCommand() = command
    def getCommandStart() = commandStart
  }

  def properCommandAt(offset : Int) : Command = {
    snapshot.node.proper_command_at(offset) match {
      case Some(command) => command
      case _ => null
    }
  }

  def revert(range : Text.Range) = snapshot.revert(range);

  def convert(range : Text.Range) = snapshot.convert(range);

  def state(command : Command) = snapshot.state(command);

  private def selectSnapshotMarkup[A](range : Text.Range, markupSelect : Markup_Tree.Select[A]) = {
    val selectStream = snapshot.select_markup(range)(markupSelect)
    someSelect(selectStream)
  }

  private def someSelect[A](stream : Stream[Text.Info[Option[A]]]) : Iterator[Text.Info[A]] =
  {
    val someFilter : Text.Info[Option[A]] => Boolean =
      ( elem ) => elem match {
        case Text.Info(range, Some(color)) => true
        case bad => false
    }
    val someTransform : Text.Info[Option[A]] => Text.Info[A] =
      ( elem ) => elem match {
        case Text.Info(range, Some(color)) => new Text.Info(range, color)
        case _ => throw new IllegalStateException("Transform after filtering - cannot happen")
    }

    stream.filter(someFilter).map(someTransform).toIterator
  }

  /*
   * Markup selectors
   */

  private def markupNameSelector(markups : Array[String]) : Markup_Tree.Select[String] =
  {
    case Text.Info(_, XML.Elem(Markup(markup, _), _)) /*if markups contains markup*/ => markup
  }

  private def markupMessageSelector(markups : Array[String]) : Markup_Tree.Select[MarkupMessage] =
  {
    case Text.Info(_, msg @ XML.Elem(Markup(markup, _), _))
    if markups contains markup => new MarkupMessage(markup, Pretty.string_of(List(msg)))
  }

  class MarkupMessage(name : String, text : String) {
    def getName() = name
    def getText() = text
  }

  private def tokensSelector[A](syntax: Outer_Syntax, commandMap : ju.Map[String,A],
                tokenMap : ju.Map[String,A]): Markup_Tree.Select[NamedData[A]] =
  {
    case Text.Info(_, XML.Elem(Markup(Markup.COMMAND, List((Markup.NAME, name))), _))
      if syntax.keyword_kind(name).isDefined => new NamedData(name, commandMap(syntax.keyword_kind(name).get))

    case Text.Info(_, XML.Elem(Markup(name, _), _))
      if tokenMap.get(name) != null => new NamedData(name, tokenMap.get(name))
  }

  class NamedData[A](name : String, data : A) {
    def getName() = name
    def getData() = data
  }

  /*
   * Markup selection methods - using the above-defined selectors
   */

  def selectMarkupNames(markups : Array[String], range : Text.Range) : ju.Iterator[Text.Info[String]] =
    selectSnapshotMarkup(range, markupNameSelector(markups))
  
  def selectMarkupMessages(markups : Array[String], range : Text.Range) : ju.Iterator[Text.Info[MarkupMessage]] =
    selectSnapshotMarkup(range, markupMessageSelector(markups))

  def selectTokens[A](syntax: Outer_Syntax, commandMap : ju.Map[String,A], tokenMap : ju.Map[String,A],
                      range : Text.Range) : ju.Iterator[Text.Info[NamedData[A]]] =
    selectSnapshotMarkup(range, tokensSelector(syntax, commandMap, tokenMap))




  def getCommands() : ju.Set[Command] = snapshot.node.commands

  def commandResults(cmd : Command, includeMarkups : Array[String], excludeMarkups : Array[String]) : ju.List[XML.Tree] = {
    val includeAll = includeMarkups.isEmpty
    snapshot.state(cmd).results.toList.map(_._2) filter {
      case XML.Elem(Markup(markup, _), _) if excludeMarkups contains markup => false
      case _ if includeAll => true
      case XML.Elem(Markup(markup, _), _) if includeMarkups contains markup => true
      case _ => false
    }
  }


}


