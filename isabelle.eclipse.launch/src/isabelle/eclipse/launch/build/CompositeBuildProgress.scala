package isabelle.eclipse.launch.build

import isabelle.Build


/**
 * Isabelle.Build progress monitor that composes multiple build progress monitors, reporting to
 * each of them.
 * 
 * @author Andrius Velykis
 */
class CompositeBuildProgress(monitors: Seq[Build.Progress]) extends Build.Progress {

  override def echo(msg: String) = monitors.foreach(_.echo(msg))

  override def theory(session: String, theory: String) =
    monitors.foreach(_.theory(session, theory))

  override def stopped = monitors.exists(_.stopped)
  
}
