package isabelle.eclipse.launch.build

import org.eclipse.core.runtime.IProgressMonitor

import isabelle.Build


/**
 * Isabelle.Build progress monitor that wraps Eclipse progress monitor to allow outputting
 * build results into Eclipse workbench.
 *
 * @author Andrius Velykis
 */
class MonitorBuildProgress(monitor: IProgressMonitor) extends Build.Progress {

  override def echo(msg: String) {
    // output as subtasks
    monitor.subTask(msg)
  }

  override def theory(session: String, theory: String) {
    echo("Building " + session + ": theory " + theory)
    // also advance the progress
    monitor.worked(10)
  }

  override def stopped = monitor.isCanceled

}
