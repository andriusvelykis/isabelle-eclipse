package isabelle.eclipse.launch.tabs

import scala.collection.mutable.Publisher

import org.eclipse.debug.core.{ILaunchConfiguration, ILaunchConfigurationWorkingCopy}
import org.eclipse.swt.widgets.Composite


/**
 * An abstraction for a launch configuration component/property
 * 
 * @author Andrius Velykis
 */
trait LaunchComponent[R] extends Publisher[R] {

  def createControl(parent: Composite)
  
  def initializeFrom(configuration: ILaunchConfiguration)
  
  def performApply(configuration: ILaunchConfigurationWorkingCopy)

  def isValid(configuration: ILaunchConfiguration,
              newConfig: Boolean): Option[Either[String, String]]
  
}
