package isabelle.eclipse.core.app

import isabelle.Build
import isabelle.Options
import isabelle.Path
import isabelle.Isabelle_System

/**
 * @author Andrius Velykis
 */
/*
 * Adapts content from Isabelle_Logic @ Isabelle/jEdit
 */
object IsabelleBuild {

  def sessions(isabellePath: String, moreSessionDirs: List[String]): List[String] = {
    
    // Before resolving sessions, reinitialise Isabelle_System at the given path.
    // This will reset correct environment variables and paths for the given Isabelle dir. 
    
    // TODO ensure that Isabelle is not running, since this may mess everything up
    // TODO allow Isabelle to be running if the same Isabelle path is used?
    Isabelle_System.init(isabellePath)
    
    // now can load options for the Isabelle system initialised above
    // TODO support custom external options?
    val initOptions = Options.init()
    
    availableSessions(moreSessionDirs, initOptions)
  }
  
  
  private def availableSessions(moreSessionDirs: List[String], options: Options): List[String] =
  {
    val dirs = moreSessionDirs.map(pathStr => (false, pathForStr(pathStr)))
    val session_tree = Build.find_sessions(options, dirs)
    val (main_sessions, other_sessions) =
      session_tree.topological_order.partition(p => p._2.groups.contains("main"))
    main_sessions.map(_._1).sorted ::: other_sessions.map(_._1).sorted
  }
  
  // TODO support Eclipse's paths and URIs? Support Eclipse variables before exploding?
  private def pathForStr(str: String): Path = Path.explode(str)
  
}
