package isabelle.eclipse.ui.util

import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.ui.progress.UIJob

/** Utilities related to the jobs API.
  *
  * @author Andrius Velykis
  */
object JobUtil {

  /** Executes the content as an UI job */
  def uiJob(name: String)(f: => Unit) {
    val job = new UIJob(name) {
      override def runInUIThread(monitor: IProgressMonitor): IStatus = {
        // execute the contents
        f
        Status.OK_STATUS
      }
    }

    job.schedule
  }

}
