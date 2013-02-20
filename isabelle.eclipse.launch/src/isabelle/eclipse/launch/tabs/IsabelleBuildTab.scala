package isabelle.eclipse.launch.tabs

import org.eclipse.jface.resource.{JFaceResources, LocalResourceManager}

import isabelle.eclipse.launch.{IsabelleLaunchImages, IsabelleLaunchPlugin}
import isabelle.eclipse.launch.config.IsabelleLaunchConstants


/**
 * Launch tab for Isabelle session build configurations.
 * 
 * Uses predefined LaunchComponents for contents.
 *
 * @author Andrius Velykis
 */
class IsabelleBuildTab extends LaunchComponentTab(IsabelleBuildTab.components) {

  override def getName = "Build"

  override def getId = IsabelleLaunchPlugin.plugin.pluginId + ".buildTab"

  // cannot access a Control here, so dispose manually in #dispose()
  private val resourceManager = new LocalResourceManager(JFaceResources.getResources)

  override def getImage = resourceManager.createImageWithDefault(IsabelleLaunchImages.TAB_BUILD)
  
  override def dispose() {
    resourceManager.dispose()
    super.dispose()
  }

}

object IsabelleBuildTab {

  def components: List[LaunchComponent[_]] = {

    val runComp = new CheckComponent(
      IsabelleLaunchConstants.ATTR_BUILD_RUN,
      "Build sessions before launch (otherwise session heaps must be already available)",
      true)

    val outputLocComp = new RadioFlagComponent(
      IsabelleLaunchConstants.ATTR_BUILD_TO_SYSTEM,
      "Build output location",
      "Build sessions to Isabelle system directory",
      "Build sessions to user home directory",
      true,
      Some(runComp))

    List(runComp, outputLocComp)
  }

}

