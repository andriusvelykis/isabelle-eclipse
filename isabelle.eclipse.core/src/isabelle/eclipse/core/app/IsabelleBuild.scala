package isabelle.eclipse.core.app

import scala.util.{Failure, Success, Try}

import org.eclipse.core.runtime.{CoreException, IPath}

import isabelle.eclipse.core.IsabelleCorePlugin

import isabelle.{Build, Isabelle_System, Options, Path}


/**
 * @author Andrius Velykis
 */
/*
 * Adapts content from Isabelle_Logic @ Isabelle/jEdit
 */
object IsabelleBuild {

  private var currentIsabelleInit: Option[IsabelleInitInfo] = None
  
  /**
   * Initialises Isabelle system at the given path.
   * 
   * Does not re-initialise if a system is already init with the given installation path and
   * environment options.
   */
  def init(isabellePath: String, envMap: Map[String, String] = Map()): Try[Unit] = {
    
    val newInit = Some(IsabelleInitInfo(isabellePath, envMap))
    // check if already initialised, then do not reinit, since it can be an expensive operation
    if (currentIsabelleInit != newInit) {
      // different init info - force Isabelle system reinitialisation
      currentIsabelleInit = newInit
      
      // ensure that Isabelle is not running, since this may mess everything up
      if (IsabelleCorePlugin.getIsabelle.isRunning) {
        Failure(new CoreException(IsabelleCorePlugin.error(
            "Isabelle is running, cannot reinitialise!", null)))
      } else {
        // wrap into Try, since exception can be thrown if the path is wrong, etc
        Try(Isabelle_System.init(isabellePath, envMap, true))        
      }
      
    } else {
      Success()
    }
  }
  
  private case class IsabelleInitInfo(val path: String, val envMap: Map[String, String])
  
  /**
   * Retrieves the list of sessions in the given Isabelle installation and additional
   * session dirs.
   * 
   * All paths must be absolute in the filesystem.
   */
  def sessions(isabellePath: String,
               moreSessionDirs: Seq[IPath],
               envMap: Map[String, String]): Try[List[String]] = {

    // Before resolving sessions, reinitialise Isabelle_System at the given path.
    // This will reset correct environment variables and paths for the given Isabelle dir. 
    init(isabellePath, envMap) flatMap { _ =>
      
      // now can load options for the Isabelle system initialised above
      val initOptions = Options.init()
      availableSessions(moreSessionDirs, initOptions)
    }
  }

  private def availableSessions(moreSessionDirs: Seq[IPath],
                                options: Options): Try[List[String]] =
  {
    val dirs = moreSessionDirs.map(path => (false, isaPath(path)))
    val session_tree = Try(Build.find_sessions(options, dirs.toList))
  
    // if session find was without issues, order them
    session_tree map { tree =>
      val (main_sessions, other_sessions) =
        tree.topological_order.partition(p => p._2.groups.contains("main"))
      main_sessions.map(_._1).sorted ::: other_sessions.map(_._1).sorted
    }
  
  }
  
  private def isaPath(path: IPath): Path = Path.explode(path.toOSString)
  
  /**
   * Checks if directory is a session dir: needs to have ROOT or ROOTs file at the top
   */
  def isSessionDir(path: IPath): Boolean = 
    path.append("ROOT").toFile.isFile || path.append("ROOTS").toFile.isFile
  
  
}
