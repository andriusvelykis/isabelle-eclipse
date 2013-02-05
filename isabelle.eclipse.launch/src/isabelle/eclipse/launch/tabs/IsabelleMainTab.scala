package isabelle.eclipse.launch.tabs

import isabelle.eclipse.launch.IsabelleLaunchImages


/**
 * Main tab for Isabelle launch configuration. Sets branding options only - the actual
 * tab content is provided via LaunchComponents.
 * 
 * @author Andrius Velykis
 */
class IsabelleMainTab(components: List[LaunchComponent[_]])
    extends LaunchComponentTab(components) {

  override def getName = "Main"
  
  override def getImage = IsabelleLaunchImages.getImage(IsabelleLaunchImages.IMG_TAB_MAIN)
  
}
