package isabelle.eclipse.launch.config

import scala.util.{Failure, Success}

import org.eclipse.core.runtime.{
  CoreException,
  IPath,
  IProgressMonitor,
  IStatus,
  MultiStatus,
  Status
}
import org.eclipse.debug.core.{DebugPlugin, ILaunch, ILaunchConfiguration}
import org.eclipse.debug.core.model.LaunchConfigurationDelegate

import LaunchConfigUtil.{configValue, pathsConfigValue}
import isabelle.Session
import isabelle.eclipse.core.IsabelleCorePlugin
import isabelle.eclipse.core.app.{Isabelle, IsabelleBuild}
import isabelle.eclipse.launch.IsabelleLaunchPlugin
import isabelle.eclipse.launch.build.IsabelleBuildJob
import isabelle.eclipse.launch.config.IsabelleLaunch._


/**
 * @author Andrius Velykis
 */
object IsabelleLaunch {

  /**
   * Constructs a status object to signal launch config error.
   *
   * The status object is built from the given message, exception causing the error,
   * error code and possibly a details message.
   *
   * @param message the status message
   * @param details detail message
   * @param exception lower level exception associated with the error
   * @param code error code
   */
  def abort[S](message: String,
            details: Option[String] = None,
            exception: Option[Throwable] = None,
            code: Int = 0): Left[IStatus, S] = {

    val severity = IStatus.ERROR
    val pluginId = IsabelleLaunchPlugin.plugin.pluginId

    val status = details match {
      case Some(detailMsg) => {
        val multiStatus = new MultiStatus(pluginId, code, message, exception.orNull)
        multiStatus.add(new Status(severity, pluginId, code, detailMsg, null));

        multiStatus
      }

      case None => new Status(severity, pluginId, code, message, exception.orNull)
    }

    Left(status)
  }

  def result[T](value: T) = Right(value)

  def availableSessions(isabellePath: String,
                        moreSessionDirs: Seq[IPath],
                        envMap: Map[String, String]): Either[IStatus, List[String]] = {
    
    val sessionsTry = IsabelleBuild.sessions(isabellePath, moreSessionDirs, envMap)

    sessionsTry match {
      case Success(sessions) => result(sessions)
      case Failure(ex) => abort(
        "Unable to initalize Isabelle at path: " + isabellePath,
        exception = Some(ex))
    }
  }


  def environmentMap(configuration: ILaunchConfiguration): 
      Either[IStatus, Map[String, String]] = {
    
    val launchMgr = DebugPlugin.getDefault.getLaunchManager
    
    try {
      val environmentVars = Option(launchMgr.getEnvironment(configuration)) getOrElse Array()
      // the environment map is read as an array of strings "key=value"
      val envMap = (environmentVars.toList map splitEnvVarString).flatten.toMap
      
      result(envMap)
      
    } catch {
      case ce: CoreException => Left(ce.getStatus)
    }
  }
  
  private def splitEnvVarString(envVarStr: String): Option[(String, String)] =
    envVarStr.split("=").toList match {
      // restore separator within value, if needed
      case key :: valueAndRest => Some(key, valueAndRest.mkString("="))
      case _ => None
    }

}

/**
 * @author Andrius Velykis
 */
abstract class IsabelleLaunch extends LaunchConfigurationDelegate {

  @throws[CoreException]
  override def launch(configuration: ILaunchConfiguration,
                      mode: String,
                      launch: ILaunch,
                      monitor: IProgressMonitor) {
    
    /**
     * Checks if cancelled and signals fail (Left), otherwise keeps the value result (Right)
     */
    def canceled: Either[IStatus, Unit] =
      if (monitor.isCanceled) Left(Status.CANCEL_STATUS) else result()
    
    /**
     * Checks if Isabelle app is not running at the moment and uses it if not
     */
    def isabelleNotRunning(): Either[IStatus, Isabelle] = {
      val isabelle = IsabelleCorePlugin.getIsabelle
      
      if (isabelle.isRunning) {
        // we only allow one prover instance
        abort("Only a single prover can be running at any time - stop the running prover before launching a new one")
      } else {
        result(isabelle)
      }
    }

    /**
     * Launches Isabelle with the given configuration and waits for it to initialise
     */
    def sessionBuild(configuration: ILaunchConfiguration,
                     isabellePath: String,
                     moreSessionDirs: Seq[IPath],
                     sessionName: String,
                     envMap: Map[String, String]): Either[IStatus, Unit] = {

      monitor.worked(3)

      val runBuild = configValue(configuration, IsabelleLaunchConstants.ATTR_BUILD_RUN, true)

      if (!runBuild) {
        // skip build - return ok result
        result()
      } else {

        monitor.subTask("Building Isabelle/" + sessionName)
        
        // run build: get other configuration options and then launch
        val buildToSystem = configValue(configuration,
          IsabelleLaunchConstants.ATTR_BUILD_TO_SYSTEM, true)

        val status = IsabelleBuildJob.syncExec(
          isabellePath, moreSessionDirs, sessionName, envMap,
          buildToSystem)

        if (status.isOK) {
          result()
        } else {
          Left(status)
        }

      }
    }
    
    /**
     * Launches Isabelle with the given configuration and waits for it to initialise
     */
    def sessionStartup(app: Isabelle,
                       isabellePath: String,
                       sessionName: String,
                       envMap: Map[String, String]): Either[IStatus, Unit] = {
      
      monitor.worked(3)
      monitor.subTask("Starting Isabelle session")
      
      val sessionTry = app.start(isabellePath, sessionName)

      sessionTry match {

        case Failure(ex) =>
          abort("Isabelle initialisation failed: " + ex.getMessage, exception = Some(ex))

        case Success(session) => waitForSessionStartup(session)
      }
    }
    
    def waitForSessionStartup(session: Session): Either[IStatus, Unit] = {

      // the session is started asynchronously, so we need to listen for it to finish.
      val phase = PhaseTracker.waitForPhaseResult(session, Set(Session.Failed, Session.Ready))
      monitor.worked(3)
      if (phase == Session.Failed) {
        val syslog = session.current_syslog()
        abort("Isabelle failed to initialise the session.", Some(syslog))
      } else {
        println("Done launching")
        result()
      }
    }
    
    monitor.subTask("Loading Isabelle launch configuration")
    
    // use Either to achieve fail-fast with error message
    val launchErr = for {
      _ <- canceled.right
      isabelle <- isabelleNotRunning().right
      _ <- canceled.right
      isabellePath <- installationPath(configuration).right
      _ <- canceled.right
      sessionDirs <- moreSessionDirs(configuration).right
      _ <- canceled.right
      envMap <- environmentMap(configuration).right
      _ <- canceled.right
      sessionName <- selectedSession(configuration, isabellePath, sessionDirs, envMap).right
      _ <- canceled.right
      _ <- sessionBuild(configuration, isabellePath, sessionDirs, sessionName, envMap).right
      _ <- canceled.right
      err <- sessionStartup(isabelle, isabellePath, sessionName, envMap).left
    } yield (err)
    
    launchErr.left foreach reportLaunchError
  }
  
  /**
   * Retrieves Isabelle installation path from the launch configuration.
   * 
   * Abstract method to allow for different configurations, e.g. dir or Mac .app
   */
  def installationPath(configuration: ILaunchConfiguration): Either[IStatus, String]
  
  
  private def moreSessionDirs(configuration: ILaunchConfiguration): Either[IStatus, Seq[IPath]] = {
    
    val sessionPaths = pathsConfigValue(configuration, IsabelleLaunchConstants.ATTR_SESSION_DIRS)
    
    val invalidPath = sessionPaths find { path => !IsabelleBuild.isSessionDir(path) }
    
    invalidPath match {
      case Some(path) => abort("Invalid Isabelle session directory (no session root): " + path)
      case None => result(sessionPaths)
    }
  }
  

  private def selectedSession(configuration: ILaunchConfiguration,
                              isabellePath: String,
                              moreSessionDirs: Seq[IPath],
                              envMap: Map[String, String]): Either[IStatus, String] = {
    
    val sessionName = configValue(configuration, IsabelleLaunchConstants.ATTR_SESSION, "")

    if (sessionName.isEmpty) {
      abort("Isabelle logic not specified")
    } else {

      val sessions = availableSessions(isabellePath, moreSessionDirs, envMap).right

      sessions flatMap (ss => if (!ss.contains(sessionName)) {
        abort("Invalid Isabelle session name specified")
      } else {
        result(sessionName)
      })
    }
  }


  private def reportLaunchError(errorStatus: IStatus) {
    // nothing to report if error status is ok, the request is simply canceled
    if (!errorStatus.isOK) {
      throw new CoreException(errorStatus)
    }
  }
  
}
