package isabelle.eclipse.launch.tabs

import org.eclipse.debug.core.{ILaunchConfiguration, ILaunchConfigurationWorkingCopy}
import org.eclipse.swt.widgets.Composite


/**
 * An abstraction for a launch configuration component/property
 * 
 * @author Andrius Velykis
 */
trait LaunchComponent[R] extends ObservableValue[R] {

  def createControl(parent: Composite, container: LaunchComponentContainer)
  
  def initializeFrom(configuration: ILaunchConfiguration)
  
  def performApply(configuration: ILaunchConfigurationWorkingCopy)

  def isValid(configuration: ILaunchConfiguration,
              newConfig: Boolean): Option[Either[String, String]]
  
  def dispose() {}
}
