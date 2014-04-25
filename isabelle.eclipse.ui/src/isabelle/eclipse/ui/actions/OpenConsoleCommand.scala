package isabelle.eclipse.ui.actions

import org.eclipse.core.commands.AbstractHandler
import org.eclipse.core.commands.ExecutionEvent
import org.eclipse.core.commands.ExecutionException
import org.eclipse.ui.console.IConsoleFactory

import isabelle.eclipse.ui.views.ProtocolConsoleFactory
import isabelle.eclipse.ui.views.RawOutputConsoleFactory
import isabelle.eclipse.ui.views.SyslogConsoleFactory


/**
 * A command to open a console for the given factory.
 *
 * @author Andrius Velykis
 */
class OpenConsoleCommand(factory: => IConsoleFactory) extends AbstractHandler {

  @throws[ExecutionException]
  override def execute(event: ExecutionEvent): AnyRef = {

    factory.openConsole()

    null
  }

}

class ProtocolConsoleCommand extends OpenConsoleCommand(new ProtocolConsoleFactory) {}

class RawOutputConsoleCommand extends OpenConsoleCommand(new RawOutputConsoleFactory) {}

class SyslogConsoleCommand extends OpenConsoleCommand(new SyslogConsoleFactory) {}
