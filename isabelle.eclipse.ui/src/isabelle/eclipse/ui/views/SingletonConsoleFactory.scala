package isabelle.eclipse.ui.views

import org.eclipse.ui.console.ConsolePlugin
import org.eclipse.ui.console.IConsole
import org.eclipse.ui.console.IConsoleFactory
import org.eclipse.ui.console.IConsoleManager


/**
 * An abstract console factory that allows only a single console per factory.
 *
 * @author Andrius Velykis
 */
abstract class SingletonConsoleFactory extends IConsoleFactory {

  override def openConsole() {
    val consoleManager = ConsolePlugin.getDefault.getConsoleManager

    val consoleOpt = consoleManager.getConsoles find (_.getType == consoleType)

    val console = consoleOpt getOrElse createAddConsole(consoleManager)

    consoleManager.showConsoleView(console)
  }

  private def createAddConsole(consoleManager: IConsoleManager): IConsole = {
      val newConsole = createConsole()
      consoleManager.addConsoles(Array(newConsole))
      newConsole
  }

  def createConsole(): IConsole

  def consoleType: String

}
