package isabelle.eclipse.launch.config

import isabelle.eclipse.launch.IsabelleLaunchPlugin

/**
 * Constants used in Isabelle launch configurations.
 *
 * @author Andrius Velykis
 */
object IsabelleLaunchConstants {

  private def pluginId = IsabelleLaunchPlugin.plugin.pluginId

  /**
   * String attribute identifying the location of Isabelle installation.
   */
  def ATTR_LOCATION = pluginId + ".ATTR_LOCATION"

  /**
   * String attribute identifying the Isabelle session.
   */
  def ATTR_SESSION = pluginId + ".ATTR_LOGIC"

}
