package isabelle.eclipse.core.app

import scala.util.{Failure, Success, Try}

import org.eclipse.core.runtime.{CoreException, IPath}

import isabelle.{Build, Isabelle_System, Options, Path}
import isabelle.eclipse.core.IsabelleCore
import isabelle.eclipse.core.internal.IsabelleCorePlugin.error


/**
 * @author Andrius Velykis
 */
/*
 * Adapts content from Isabelle_Logic @ Isabelle/jEdit
 */
object IsabelleBuild {

  private var currentIsabelleInit: Option[IsabelleInitInfo] = None
  
  def isInit = currentIsabelleInit.isDefined

  /**
   * Initialises Isabelle system at the given path.
   *
   * Does not re-initialise if a system is already init with the given installation path and
   * environment options.
   */
  def init(isabellePath: String,
           envMap: Map[String, String] = Map(),
           systemProperties: Map[String, String] = Map()): Try[Unit] = {
    
    val newInit = Some(IsabelleInitInfo(isabellePath, envMap, systemProperties))
    // check if already initialised, then do not reinit, since it can be an expensive operation
    if (currentIsabelleInit != newInit) {
      // different init info - force Isabelle system reinitialisation
      
      // ensure that Isabelle is not running, since this may mess everything up
      if (IsabelleCore.isabelle.isRunning) {
        Failure(new CoreException(error(msg = Some("Isabelle is running, cannot reinitialise!"))))
      } else {
        
        // set the system properties before initialisation, e.g. Cygwin path
        // TODO somehow remove previously set properties, e.g. to avoid contamination?
        systemProperties foreach Function.tupled(System.setProperty)

        // wrap into Try, since exception can be thrown if the path is wrong, etc
        val initResult = Try(Isabelle_System.init(isabellePath, envMap, true))
        
        // if success, mark as current init
        initResult foreach { _ => currentIsabelleInit = newInit }
        
        initResult
      }
      
    } else {
      Success()
    }
  }

  private case class IsabelleInitInfo(val path: String,
                                      val envMap: Map[String, String],
                                      val systemProperties: Map[String, String])
  
  /**
   * Retrieves the list of sessions in the given Isabelle installation and additional
   * session dirs.
   * 
   * All paths must be absolute in the filesystem.
   */
  def sessions(isabellePath: String,
               moreSessionDirs: Seq[IPath],
               envMap: Map[String, String],
               systemProperties: Map[String, String]): Try[List[String]] = {

    // Before resolving sessions, reinitialise Isabelle_System at the given path.
    // This will reset correct environment variables and paths for the given Isabelle dir. 
    init(isabellePath, envMap, systemProperties) flatMap { _ =>
      
      // now can load options for the Isabelle system initialised above
      val initOptions = Options.init()
      availableSessions(moreSessionDirs, initOptions)
    }
  }

  private def availableSessions(moreSessionDirs: Seq[IPath],
                                options: Options): Try[List[String]] =
  {
    val dirs = resolvePaths(moreSessionDirs)
    val session_tree = Try(Build.find_sessions(options, dirs.toList))
  
    // if session find was without issues, order them
    session_tree map { tree =>
      val (main_sessions, other_sessions) =
        tree.topological_order.partition(p => p._2.groups.contains("main"))
      main_sessions.map(_._1).sorted ::: other_sessions.map(_._1).sorted
    }
  
  }
  
  def resolvePaths(paths: Seq[IPath]): Seq[(Boolean, Path)] =
    paths.map(path => (false, isaPath(path)))
  
  private def isaPath(path: IPath): Path = Path.explode(path.toOSString)
  
  /**
   * Checks if directory is a session dir: needs to have ROOT or ROOTs file at the top
   */
  def isSessionDir(path: IPath): Boolean = 
    path.append("ROOT").toFile.isFile || path.append("ROOTS").toFile.isFile
  
  
  def sessionContent(moreSessionDirs: Seq[IPath],
      sessionName: String,
      inlinedFiles: Boolean): Try[Build.Session_Content] = {
    
    val dirs = moreSessionDirs map isaPath
    Try(Build.session_content(inlinedFiles, dirs.toList, sessionName).check_errors)
  }
  
}
