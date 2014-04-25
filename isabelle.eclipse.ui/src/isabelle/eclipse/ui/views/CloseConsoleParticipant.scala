package isabelle.eclipse.ui.views

import org.eclipse.ui.console.IConsole
import org.eclipse.ui.console.IConsoleConstants
import org.eclipse.ui.console.IConsolePageParticipant
import org.eclipse.ui.console.actions.CloseConsoleAction
import org.eclipse.ui.part.IPageBookViewPage


/**
 * A console participant that adds a "Close" button for the console.
 * 
 * @author Andrius Velykis
 */
class CloseConsoleParticipant extends IConsolePageParticipant {

  override def getAdapter(adapter: Class[_]): AnyRef = null

  override def init(page: IPageBookViewPage, console: IConsole) {
    val action = new CloseConsoleAction(console)
    val toolbar = page.getSite.getActionBars.getToolBarManager
    toolbar.appendToGroup(IConsoleConstants.LAUNCH_GROUP, action)
  }

  override def dispose() {}

  override def activated() {}

  override def deactivated() {}

}
