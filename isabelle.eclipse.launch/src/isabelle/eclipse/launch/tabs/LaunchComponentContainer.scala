package isabelle.eclipse.launch.tabs

import org.eclipse.swt.widgets.{Button, Composite}


/**
 * Facade for launch container (e.g. launch configuration tab): allows access to some methods
 * in the tab class.
 *
 * @author Andrius Velykis
 */
trait LaunchComponentContainer {

  def createPushButton(parent: Composite, label: String): Button
  
  def createRadioButton(parent: Composite, label: String): Button
  
  def createCheckButton(parent: Composite, label: String): Button
  
  def update()

}
