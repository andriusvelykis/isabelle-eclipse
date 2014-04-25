package isabelle.eclipse.ui.views

import org.eclipse.ui.console.ConsolePlugin
import org.eclipse.ui.console.IConsole
import org.eclipse.ui.console.IConsoleFactory


/**
 * An abstract console factory that allows only a single console per factory.
 *
 * @author Andrius Velykis
 */
abstract class SingletonConsoleFactory extends IConsoleFactory {

  var console: Option[IConsole] = None

  override def openConsole() {
    val consoleManager = ConsolePlugin.getDefault.getConsoleManager

    val consoleExists = console exists consoleManager.getConsoles.contains

    if (!consoleExists) {
      if (console.isEmpty) {
        console = Some(createConsole())
      }

      consoleManager.addConsoles(console.toArray)
    }

    consoleManager.showConsoleView(console.get)
  }

  def createConsole(): IConsole

}
