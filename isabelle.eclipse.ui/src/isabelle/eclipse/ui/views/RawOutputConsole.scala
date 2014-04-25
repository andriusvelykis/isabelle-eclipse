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
import isabelle.eclipse.core.util.LoggingActor
import isabelle.eclipse.core.util.SessionEvents
import isabelle.eclipse.ui.internal.IsabelleImages
import isabelle.eclipse.ui.internal.IsabelleUIPlugin
import isabelle.eclipse.ui.internal.IsabelleUIPlugin.error
import isabelle.eclipse.ui.internal.IsabelleUIPlugin.log


/**
 * A message console that tracks Isabelle Raw Output messages when initialised.
 * 
 * @author Andrius Velykis
 */
class RawOutputConsole(name: String, consoleType: String, image: ImageDescriptor) 
    extends MessageConsole(name, consoleType, image, true) with SessionEvents {

  // the actor to react to session events
  override protected val sessionActor = LoggingActor {
    loop {
      react {
        case output: Isabelle_Process.Output =>
          consoleStream.print(XML.content(output.message))
          if (!output.is_stdout && !output.is_stderr) consoleStream.println()

        case bad => System.err.println("RawOutputConsole: ignoring bad message " + bad)
      }
    }
  }

  // subscribe to commands change session events
  override protected def sessionEvents(session: Session) = List(session.raw_output_messages)
  
  private lazy val consoleStream = newMessageStream()

  override protected def init() {
    super.init()

    consoleStream.println("Starting Raw Output Console")
    initSessionEvents()
  }


  override protected def dispose() {
    disposeSessionEvents()

    Try(consoleStream.close()).failed foreach ( ex =>
      log(error(Some(ex), Some("Unable to close raw output console"))) )

    super.dispose()
  }

}

class RawOutputConsoleFactory extends SingletonConsoleFactory {

  val consoleType = IsabelleUIPlugin.plugin.pluginId + ".rawOutputConsole"

  override def createConsole(): IConsole =
    new RawOutputConsole("Isabelle Raw Output", consoleType, IsabelleImages.RAW_OUTPUT_CONSOLE)

}
