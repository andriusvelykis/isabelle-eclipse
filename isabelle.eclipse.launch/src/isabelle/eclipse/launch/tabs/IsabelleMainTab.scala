package isabelle.eclipse.launch.tabs

import org.eclipse.jface.resource.{JFaceResources, LocalResourceManager}

import isabelle.eclipse.launch.{IsabelleLaunchImages, IsabelleLaunchPlugin}


/**
 * Main tab for Isabelle launch configuration. Sets branding options only - the actual
 * tab content is provided via LaunchComponents.
 * 
 * @author Andrius Velykis
 */
class IsabelleMainTab(components: List[LaunchComponent[_]])
    extends LaunchComponentTab(components) {

  override def getName = "Main"
    
  override def getId = IsabelleLaunchPlugin.plugin.pluginId + ".mainTab"

  // cannot access a Control here, so dispose manually in #dispose()
  private val resourceManager = new LocalResourceManager(JFaceResources.getResources)

  override def getImage = resourceManager.createImageWithDefault(IsabelleLaunchImages.TAB_MAIN)
  
  override def dispose() {
    resourceManager.dispose()
    super.dispose()
  }

}
