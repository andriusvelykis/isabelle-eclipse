package isabelle.eclipse.core

import isabelle.eclipse.core.internal.IsabelleCorePlugin


/**
 * Central facade for main resources within the Isabelle Core plugin.
 *
 * @author Andrius Velykis
 */
object IsabelleCore {

  def isabelle = IsabelleCorePlugin.plugin.isabelle

}
