package isabelle.eclipse.launch.build

import scala.concurrent.Await
import scala.concurrent.Promise
import scala.concurrent.duration.Duration
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.jface.action.Action
import org.eclipse.ui.PlatformUI
import org.eclipse.ui.progress.IProgressConstants

import isabelle.Build
import isabelle.Isabelle_System
import isabelle.Options
import isabelle.eclipse.core.app.IsabelleBuild
import isabelle.eclipse.core.app.IsabelleBuild.IsabellePaths
import isabelle.eclipse.launch.IsabelleLaunchImages
import isabelle.eclipse.launch.config.IsabelleLaunch.abort


/**
 * Eclipse job to wrap Isabelle session build process and report the progress to the workbench.
 * 
 * Provides a method to simulate running the job synchronously.
 * 
 * @author Andrius Velykis
 */
object IsabelleBuildJob {

  /**
   * Runs Isabelle build in a new job and blocks the current thread.
   */
  def syncExec(isabellePath: IsabellePaths,
               moreSessionDirs: Seq[IPath],
               sessionName: String,
               buildToSystem: Boolean = true): IStatus = {

    val buildPromise = Promise[IStatus]()

    val job = new IsabelleBuildJob(
      isabellePath, moreSessionDirs, sessionName,
      buildToSystem) {
      
      override protected def run(monitor: IProgressMonitor): IStatus = {
        val result = super.run(monitor)
        buildPromise.success(result)

        // we can output an error here no problem: Eclipse will combine both into a single
        // dialog which looks good.
        result
      }
    }
    
    job.schedule()
    
    // await on the future and block until the required phase is reached - then return it
    val future = buildPromise.future
    val resultStatus = Await.result(future, Duration.Inf)
    
    resultStatus
  }
}


/**
 * Eclipse job to wrap Isabelle session build process and report the progress to the workbench.
 * 
 * @author Andrius Velykis
 */
class IsabelleBuildJob(isabellePath: IsabellePaths,
                       moreSessionDirs: Seq[IPath],
                       sessionName: String,
                       buildToSystem: Boolean = true)
    extends Job("Building Isabelle/" + sessionName) {
  
//  setUser(true)
  setProperty(IProgressConstants.KEEP_PROPERTY, true)
  setProperty(IProgressConstants.ICON_PROPERTY, IsabelleLaunchImages.JOB_BUILD)
  
  private val logBuffer = new StringBuffer
  private var mlIdentifier: Option[String] = None

  /**
   * Contributed action to display build log in a dialog (available in various places of
   * the Job framework in Eclipse workbench).
   */
  private val showLogAction = new Action("View build log...") {
    override def run() {
      // show the results
      
      val mlIdStr = mlIdentifier.map(" (" + _ + ")") getOrElse ""
      
      val shell = PlatformUI.getWorkbench.getActiveWorkbenchWindow.getShell
      val logDialog = new LogDialog(shell,
        "Isabelle Build Log",
        "Isabelle/" + sessionName + mlIdStr + " session build log:",
        logBuffer.toString)
      logDialog.setBlockOnOpen(false)
      
      logDialog.open()
    }
  }
  setProperty(IProgressConstants.ACTION_PROPERTY, showLogAction)
  
  
  override def belongsTo(family: Any): Boolean =
    family == ResourcesPlugin.FAMILY_MANUAL_BUILD


  override protected def run(monitor: IProgressMonitor): IStatus = {

    // TODO determine job length?
    monitor.beginTask(getName, IProgressMonitor.UNKNOWN)

    // track the build progress both in progress monitor and in the log buffer
    val monitorProgress = new MonitorBuildProgress(monitor)
    val textProgress = new StringBuildProgress(logBuffer.append)
    val buildProgress = new CompositeBuildProgress(List(monitorProgress, textProgress))

    val dirs = IsabelleBuild.resolvePaths(moreSessionDirs)

    // init Isabelle system and launch the build process
    val buildTry = IsabelleBuild.init(isabellePath) flatMap { _ =>
      
      // try to retrieve ML identifier for log info (only after init)
      val mlId = Isabelle_System.getenv("ML_IDENTIFIER")
      mlIdentifier = if (mlId.isEmpty) None else Some(mlId)

      // TODO reuse options somehow?
      val options = Options.init()
      // do the build
      Try(Build.build(
        buildProgress,
        options,
        build_heap = true,
        verbose = true,
        more_dirs = dirs.toList,
        system_mode = buildToSystem,
        sessions = List(sessionName)))
    }

    // set the last task as "show log" action,
    // since it will be the one displayed finally in the UI after the job is finished
    monitor.subTask("")
    monitor.setTaskName(showLogAction.getText)
    

    // wrap the result or exception into an Eclipse status
    val status = buildTry match {

      case Failure(ex) =>
        abort("Building Isabelle session failed: " + ex.getMessage,
            Some(logBuffer.toString),
            Some(ex))

      case Success(rc) => rc match {
        // return code 0: ok!
        case 0 => Left(Status.OK_STATUS)
        
        case _ if (monitor.isCanceled) => Left(Status.CANCEL_STATUS)
        
        case _ =>
          abort("Building Isabelle session failed (return code " + rc + ")",
              Some(logBuffer.toString))
      }
    }
    
    // only statuses returned, so unpack
    status.left.get
  }
  
}
