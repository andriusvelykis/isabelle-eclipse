package isabelle.eclipse.ui.editors

import scala.util.Try

import org.eclipse.jface.text.{DocumentCommand, IAutoEditStrategy, IDocument}

import isabelle.Symbol
import isabelle.eclipse.core.IsabelleCore


/**
 * Auto-edit strategy that decodes Isabelle ASCII symbols when adding them to text.
 * 
 * The auto-edit is invoked when whitespace character is typed (affects the last word)
 * and when pasting multiple characters.
 * 
 * @author Andrius Velykis
 */
class IsabelleAutoEditStrategy extends IAutoEditStrategy {

  private def symbolInit = IsabelleCore.isabelle.isInit

  override def customizeDocumentCommand(document: IDocument, command: DocumentCommand) =
    if (symbolInit && isAutoEditCommand(command)) {

      // join with word before the edit (e.g. to transform the word)
      val wordBefore = lastWordAt(document, command.offset)
      val decoded = Symbol.decode(wordBefore + command.text)

      val (offset, length, text) = if (decoded.startsWith(wordBefore)) {
        // do not replace the word before
        (command.offset, command.length, decoded.substring(wordBefore.length))
      } else {
        // extend edit command to replace the word before
        (command.offset - wordBefore.length, command.length + wordBefore.length, decoded)
      }

      command.offset = offset
      command.length = length
      command.text = text
    }

  /**
   * Do auto edit only if the whitespace is entered (e.g. a space after a word) or when
   * multiple characters are added (e.g. pasting text).
   */
  private def isAutoEditCommand(command: DocumentCommand) = {
    val whitespaceEdit = command.text.length == 1 && command.text.charAt(0).isWhitespace
    
    whitespaceEdit || command.text.length > 1
  }
  
  private def whitespaceEdit(command: DocumentCommand): Boolean =
    command.text.exists (_.isWhitespace)

  private def lastWordAt(document: IDocument, offset: Int): String = {
    val lineText = lineTextAt(document, offset)
    lineText.toOption.map(lastWord) getOrElse ""
  }

  private def lineTextAt(document: IDocument, offset: Int) = Try {
    val line = document.getLineOfOffset(offset)
    val start = document.getLineOffset(line)
    val text = document.get(start, offset - start)
    text
  }

  private def lastWord(text: String) =
    text.reverseIterator.takeWhile(c => !c.isWhitespace).mkString.reverse
}
