package isabelle.eclipse.launch.tabs

import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.debug.ui.EnvironmentTab

import isabelle.eclipse.launch.config.IsabelleLaunch


/**
 * An extension of EnvironmentTab to access the environment variables within the dialog.
 * 
 * @author Andrius Velykis
 */
class IsabelleEnvironmentTab extends EnvironmentTab with ObservableValue[Map[String, String]] {

  private var config: ILaunchConfiguration = _;
  
  override def initializeFrom(configuration: ILaunchConfiguration) {
    super.initializeFrom(configuration)
    // store the config to reuse when reading the environment variables
    this.config = configuration
  }
  
  def envMap: Map[String, String] = {
    
    val dummyWorkingCopy = config.getWorkingCopy
    
    performApply(dummyWorkingCopy)
    
    IsabelleLaunch.environmentMap(dummyWorkingCopy).right.toOption getOrElse Map()
  }
  
  override def value = envMap
  
  override def updateLaunchConfigurationDialog() {
    super.updateLaunchConfigurationDialog()
    
    publish(envMap)
  }
  
}
