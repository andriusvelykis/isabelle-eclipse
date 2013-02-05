package isabelle.eclipse.launch.config

import org.eclipse.core.runtime.{CoreException, IProgressMonitor, IStatus, MultiStatus, Status}
import org.eclipse.debug.core.{ILaunch, ILaunchConfiguration}
import org.eclipse.debug.core.model.LaunchConfigurationDelegate

import LaunchConfigUtil.configValue
import isabelle.eclipse.core.IsabelleCorePlugin
import isabelle.eclipse.core.app.{Isabelle, IsabelleBuild}
import isabelle.eclipse.launch.IsabelleLaunchPlugin

// import helper methods
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

  def availableSessions(isabellePath: String): Either[IStatus, List[String]] =
    try {
      // FIXME additional session dirs
      val sessions = IsabelleBuild.sessions(isabellePath, Nil)
      result(sessions)
    } catch {
      case ex: Exception =>
        abort("Unable to launch Isabelle at path: " + isabellePath, exception = Some(ex))
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
      if (monitor.isCanceled) Left(Status.OK_STATUS) else result()
    
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
    def sessionStartup(app: Isabelle, isabellePath: String, sessionName: String): Either[IStatus, Unit] = {
      
      monitor.beginTask("Launching " + configuration.getName() + "...", IProgressMonitor.UNKNOWN)
      
      val session = app.start(isabellePath, sessionName)
  
      // the session is started asynchronously, so we need to listen for it to finish.
      val phase = PhaseTracker.waitForPhaseResult(session)
      if (PhaseTracker.isFailedPhase(phase)) {
        val syslog = session.current_syslog()
        abort("Isabelle failed to initialise the session.", Some(syslog))
      } else {
        println("Done launching")
        result()
      }
    }
    
    // use Either to achieve fail-fast with error message
    val launchErr = for {
      _ <- canceled.right
      isabelle <- isabelleNotRunning().right
      _ <- canceled.right
      isabellePath <- installationPath(configuration).right
      _ <- canceled.right
      sessionName <- selectedSession(configuration, isabellePath).right
      _ <- canceled.right
      err <- sessionStartup(isabelle, isabellePath, sessionName).left
    } yield (err)
    
    launchErr.left foreach reportLaunchError
  }
  
  /**
   * Retrieves Isabelle installation path from the launch configuration.
   * 
   * Abstract method to allow for different configurations, e.g. dir or Mac .app
   */
  def installationPath(configuration: ILaunchConfiguration): Either[IStatus, String]

  private def selectedSession(configuration: ILaunchConfiguration,
                              isabellePath: String): Either[IStatus, String] = {
    
    val sessionName = configValue(configuration, IsabelleLaunchConstants.ATTR_SESSION, "")

    if (sessionName.isEmpty) {
      abort("Isabelle logic not specified")
    } else {

      val sessions = availableSessions(isabellePath).right

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
