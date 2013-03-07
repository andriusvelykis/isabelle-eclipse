package isabelle.eclipse.launch.build

import isabelle.Build


/**
 * Isabelle.Build progress monitor that wraps Eclipse progress monitor to allow outputting
 * build results into Eclipse workbench.
 *
 * @author Andrius Velykis
 */
class StringBuildProgress(out: String => Any) extends Build.Progress {

  override def echo(msg: String) = {
    out(msg + "\n")
  }

  override def theory(session: String, theory: String) =
    echo(session + ": theory " + theory)

}
