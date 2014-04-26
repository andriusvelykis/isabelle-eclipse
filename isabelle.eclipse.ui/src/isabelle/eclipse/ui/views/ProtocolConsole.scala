package isabelle.eclipse.ui.views

import scala.actors.Actor.loop
import scala.actors.Actor.react
import scala.util.Try

import org.eclipse.jface.resource.ImageDescriptor
import org.eclipse.ui.console.IConsole
import org.eclipse.ui.console.MessageConsole

import isabelle.Isabelle_Process
import isabelle.Session
import isabelle.eclipse.core.util.LoggingActor
import isabelle.eclipse.core.util.SessionEvents
import isabelle.eclipse.ui.internal.IsabelleImages
import isabelle.eclipse.ui.internal.IsabelleUIPlugin
import isabelle.eclipse.ui.internal.IsabelleUIPlugin.error
import isabelle.eclipse.ui.internal.IsabelleUIPlugin.log


/**
 * A message console that tracks all Isabelle messages when initialised.
 * 
 * @author Andrius Velykis
 */
class ProtocolConsole(name: String, consoleType: String, image: ImageDescriptor) 
    extends MessageConsole(name, consoleType, image, true) with SessionEvents {

  // the actor to react to session events
  override protected val sessionActor = LoggingActor {
    loop {
      react {
        case input: Isabelle_Process.Input =>
          consoleStream.println(input.toString)

        case output: Isabelle_Process.Output =>
          consoleStream.println(output.message.toString)

        case bad => System.err.println("Protocol console: ignoring bad message " + bad)
      }
    }
  }

  // subscribe to commands change session events
  override protected def sessionEvents(session: Session) = List(session.all_messages)
  
  private lazy val consoleStream = newMessageStream()

  override protected def init() {
    super.init()

    consoleStream.println("Starting Protocol Console")
    initSessionEvents()
  }


  override protected def dispose() {
    disposeSessionEvents()

    Try(consoleStream.close()).failed foreach ( ex =>
      log(error(Some(ex), Some("Unable to close protocol console"))) )

    super.dispose()
  }

}

class ProtocolConsoleFactory extends SingletonConsoleFactory {

  val consoleType = IsabelleUIPlugin.plugin.pluginId + ".protocolConsole"

  override def createConsole(): IConsole =
    new ProtocolConsole("Isabelle Protocol Messages", consoleType, IsabelleImages.PROTOCOL_CONSOLE)

}

