package isabelle.eclipse.ui.text.hyperlink

import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.hyperlink.IHyperlink

import isabelle.Document_ID
import isabelle.Session


/**
 * A hyperlink that provides dialog functionality via the Isabelle document.
 *
 * Isabelle backend can open dialogs which are rendered in prover output as hyperlinks
 * and the user can select the appropriate option via this hyperlink.
 *
 * Can be tested using the following ML dialog:
 * ```
 * ML_command {*
 *   val (markup, promise) = Active.dialog_text ();
 *   writeln ("Something went utterly wrong!  " ^
 *     commas
 *       [markup "Abort",
 *        markup "Retry",
 *        markup "Ignore",
 *        markup "Fail"] ^ "?");
 *   writeln (Future.join promise);
 * *}
 * ```
 *
 * @author Andrius Velykis
 */
class ProtocolDialogHyperlink(linkRegion: IRegion,
                              session: => Option[Session],
                              id: Document_ID.Generic,
                              serial: Long,
                              result: String,
                              targetName: Option[String] = Some("Send Selection to Isabelle"))
    extends IHyperlink {

  override def getHyperlinkRegion(): IRegion = linkRegion

  override def getTypeLabel = "Send Selection to Isabelle"

  override def getHyperlinkText(): String = targetName.orNull

  override def open() =
    session foreach (_.dialog_result(id, serial, result))

}
