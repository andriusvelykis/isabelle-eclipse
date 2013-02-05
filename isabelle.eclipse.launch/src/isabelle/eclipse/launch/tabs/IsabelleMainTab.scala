package isabelle.eclipse.launch.tabs

import org.eclipse.jface.resource.{JFaceResources, LocalResourceManager}

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

  lazy private val resourceManager =
    new LocalResourceManager(JFaceResources.getResources, getControl)

  override def getImage = resourceManager.createImageWithDefault(IsabelleLaunchImages.TAB_MAIN)

}
