
package isabelle.scala

import java.{ util => ju }
import scala.collection.JavaConversions._

import isabelle._
import Document.Snapshot
import Markup_Tree.Select

object SnapshotUtil {

  private def selectSnapshotMarkup[A](snapshot: Snapshot, range: Text.Range, markupSelect: Select[A]) = {
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

  private def markupNameSelector(markups : Array[String]) : Select[String] =
  {
    case Text.Info(_, XML.Elem(Markup(markup, _), _)) if markups contains markup => markup
  }

  private def markupMessageSelector(markups : Array[String]) : Select[MarkupMessage] =
  {
    case Text.Info(_, msg @ XML.Elem(Markup(markup, _), body))
    if markups contains markup => {
      val legacy = 
        body match {
          case List(XML.Elem(Markup(Markup.LEGACY, _), _)) => true
          case _ => false
        }
      
      new MarkupMessage(markup, Pretty.string_of(List(msg)), legacy)
    }
  }

  class MarkupMessage(name : String, text : String, legacy : Boolean) {
    def getName() = name
    def getText() = text
    def isLegacy() = legacy
  }

  /*
   * Markup selection methods - using the above-defined selectors
   */

  def selectMarkupNames(snapshot : Snapshot, markups : Array[String], range : Text.Range) : Iterator[Text.Info[String]] =
    selectSnapshotMarkup(snapshot, range, markupNameSelector(markups))
  
  def selectMarkupMessages(snapshot : Snapshot, markups : Array[String], range : Text.Range) : Iterator[Text.Info[MarkupMessage]] =
    selectSnapshotMarkup(snapshot, range, markupMessageSelector(markups))

  def getCommandResults(snapshot : Snapshot, cmd : Command, 
      includeMarkups : Array[String], excludeMarkups : Array[String]) : ju.List[XML.Tree] = {
    val includeAll = includeMarkups.isEmpty
    snapshot.state.command_state(snapshot.version, cmd).results.toList.map(_._2) filter {
      case XML.Elem(Markup(markup, _), _) if excludeMarkups contains markup => false
      case _ if includeAll => true
      case XML.Elem(Markup(markup, _), _) if includeMarkups contains markup => true
      case _ => false
    }
  }


}


