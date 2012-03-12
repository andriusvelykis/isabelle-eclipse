package isabelle.eclipse.core.util;

import isabelle.eclipse.core.IsabelleCorePlugin;
import isabelle.scala.SessionActor;

/**
 * A session actor that handles all exceptions by logging them to plug-in log
 * 
 * @author Andrius Velykis
 */
public class SafeSessionActor extends SessionActor {

	@Override
	public void handleException(Exception e) {
		// log exception (parent rethrows it)
		IsabelleCorePlugin.log(e);
	}

}
