package isabelle.eclipse.ui.text.hyperlink

import org.eclipse.core.runtime.{IProgressMonitor, IStatus, Status}
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.hyperlink.IHyperlink

import isabelle.{File, Isabelle_System, XML}


/**
 * A hyperlink that opens a theory graph browser with link contents.
 *
 * Currently delegates the UI to "isabelle browser" command.
 * TODO implement Eclipse-based equivalent theory graph browser?
 * 
 * Used in dependency commands, e.g. `class_deps` or `code_deps`
 *
 * @author Andrius Velykis
 */
class TheoryGraphHyperlink(linkRegion: IRegion,
                           graphContent: XML.Body,
                           targetName: Option[String] = Some("Open in Graph Browser"))
    extends IHyperlink {

  override def getHyperlinkRegion(): IRegion = linkRegion

  override def getTypeLabel = "Open in Graph Browser"

  override def getHyperlinkText(): String = targetName.orNull


  override def open() {

    val showBrowserJob = new Job("Opening graph browser") {
      override protected def run(monitor: IProgressMonitor): IStatus = {
        showGraphBrowser()
        Status.OK_STATUS
      }
    }

    showBrowserJob.schedule()
  }


  private def showGraphBrowser() {
    val graphTempFile = File.tmp_file("graph")
    File.write(graphTempFile, XML.content(graphContent))
    Isabelle_System.bash_env(null,
      Map("GRAPH_FILE" -> Isabelle_System.posix_path(graphTempFile)),
      "\"$ISABELLE_TOOL\" browser -c \"$GRAPH_FILE\" &")
  }

}
