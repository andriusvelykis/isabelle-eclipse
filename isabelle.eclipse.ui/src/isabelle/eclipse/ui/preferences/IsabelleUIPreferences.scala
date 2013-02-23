package isabelle.eclipse.ui.preferences

import org.eclipse.core.runtime.Platform
import org.eclipse.core.runtime.preferences.{
  AbstractPreferenceInitializer,
  DefaultScope,
  IScopeContext,
  InstanceScope
}

import isabelle.eclipse.ui.IsabelleUIPlugin


/**
  * @author Andrius Velykis 
  */
object IsabelleUIPreferences {
  
  private def pluginId = IsabelleUIPlugin.PLUGIN_ID
  
  private[preferences] def prefNode(prefScope: IScopeContext) = prefScope.getNode(pluginId)
  
  // Instance scope by itself does not include default values.
  // Need to use IPreferencesService to access values - but can use the prefs for listeners
  lazy val prefs = prefNode(InstanceScope.INSTANCE)
  
  /** Retrieves Boolean value for Isabelle UI preferences (supports Default values) */
  def getBoolean(key: String, default: Boolean) =
    Platform.getPreferencesService.getBoolean(pluginId, key, default, null)
  
  
}

/**
  * @author Andrius Velykis 
  */
class IsabelleUIPreferenceInitializer extends AbstractPreferenceInitializer {
  
  override def initializeDefaultPreferences() {
    
    import IsabelleUIPreferences._
    
    val prefDefaults = prefNode(DefaultScope.INSTANCE)
    
    
    ColourPreferenceInitializer.initializeDefaultPreferences()
  }
}
