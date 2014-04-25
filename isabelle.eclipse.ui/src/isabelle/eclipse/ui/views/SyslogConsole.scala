package isabelle.eclipse.ui.views

import scala.actors.Actor.loop
import scala.actors.Actor.react
import scala.util.Try

import org.eclipse.jface.resource.ImageDescriptor
import org.eclipse.ui.console.IConsole
import org.eclipse.ui.console.MessageConsole

import isabelle.Isabelle_Process
import isabelle.Session
import isabelle.XML
import isabelle.eclipse.core.IsabelleCore
import isabelle.eclipse.core.util.LoggingActor
import isabelle.eclipse.core.util.SessionEvents
import isabelle.eclipse.ui.internal.IsabelleImages
import isabelle.eclipse.ui.internal.IsabelleUIPlugin
import isabelle.eclipse.ui.internal.IsabelleUIPlugin.error
import isabelle.eclipse.ui.internal.IsabelleUIPlugin.log


/**
 * A message console that tracks Isabelle system log messages when initialised.
 * It also loads current syslog upon initialisation.
 * 
 * @author Andrius Velykis
 */
class SyslogConsole(name: String, consoleType: String, image: ImageDescriptor) 
    extends MessageConsole(name, consoleType, image, true) with SessionEvents {

  // the actor to react to session events
  override protected val sessionActor = LoggingActor {
    loop {
      react {
        case output: Isabelle_Process.Output =>
          if (output.is_syslog) consoleStream.println(XML.content(output.message))

        case bad => java.lang.System.err.println("Syslog console: ignoring bad message " + bad)
      }
    }
  }

  // subscribe to commands change session events
  override protected def sessionEvents(session: Session) = List(session.syslog_messages)

  override protected def sessionInit(session: Session) {
    showCurrentLog()
  }
  
  private lazy val consoleStream = newMessageStream()
  
  private def showCurrentLog() {
    val currentLog = IsabelleCore.isabelle.session map (_.current_syslog)
    currentLog foreach consoleStream.println
  }

  override protected def init() {
    super.init()

    initSessionEvents()
  }


  override protected def dispose() {
    disposeSessionEvents()

    Try(consoleStream.close()).failed foreach ( ex =>
      log(error(Some(ex), Some("Unable to close raw output console"))) )

    super.dispose()
  }

}

class SyslogConsoleFactory extends SingletonConsoleFactory {

  val consoleType = IsabelleUIPlugin.plugin.pluginId + ".syslogConsole"

  override def createConsole(): IConsole =
    new SyslogConsole("Isabelle System Log", consoleType, IsabelleImages.RAW_OUTPUT_CONSOLE)

}
