package isabelle.eclipse.core.app

import org.eclipse.core.runtime.IPath

import isabelle.{Build, Isabelle_System, Options, Path}


/**
 * @author Andrius Velykis
 */
/*
 * Adapts content from Isabelle_Logic @ Isabelle/jEdit
 */
object IsabelleBuild {

  /**
   * Retrieves the list of sessions in the given Isabelle installation and additional
   * session dirs.
   * 
   * All paths must be absolute in the filesystem.
   */
  def sessions(isabellePath: String,
               moreSessionDirs: Seq[IPath],
               envMap: Map[String, String]): List[String] = {
    
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
  
  
  private def availableSessions(moreSessionDirs: Seq[IPath], options: Options): List[String] =
  {
    val dirs = moreSessionDirs.map(path => (false, isaPath(path)))
    val session_tree = Build.find_sessions(options, dirs.toList)
    val (main_sessions, other_sessions) =
      session_tree.topological_order.partition(p => p._2.groups.contains("main"))
    main_sessions.map(_._1).sorted ::: other_sessions.map(_._1).sorted
  }
  
  private def isaPath(path: IPath): Path = Path.explode(path.toOSString)
  
  /**
   * Checks if directory is a session dir: needs to have ROOT or ROOTs file at the top
   */
  def isSessionDir(path: IPath): Boolean = 
    path.append("ROOT").toFile.isFile || path.append("ROOTS").toFile.isFile
  
  
}
