package isabelle.eclipse.ui.views

import scala.actors.Actor._
import scala.util.Try

import org.eclipse.jface.resource.ImageDescriptor
import org.eclipse.ui.console.MessageConsole

import isabelle.{Isabelle_Process, Session, XML}
import isabelle.eclipse.core.util.{LoggingActor, SessionEvents}
import isabelle.eclipse.ui.IsabelleUIPlugin


/**
 * A message console that tracks Isabelle Raw Output messages when initialised.
 * 
 * @author Andrius Velykis
 */
class RawOutputConsole(name: String, image: ImageDescriptor) 
    extends MessageConsole(name, image, true) with SessionEvents {

  // the actor to react to session events
  override protected val sessionActor = LoggingActor {
    loop {
      react {
        case output: Isabelle_Process.Output =>
          if (output.is_stdout || output.is_stderr)
            consoleStream.print(XML.content(output.message))

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

    Try(consoleStream.close()).failed foreach (
      IsabelleUIPlugin.log("Unable to close raw output console", _))

    super.dispose()
  }

}
