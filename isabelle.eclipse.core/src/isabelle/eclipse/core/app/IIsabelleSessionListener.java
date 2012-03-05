package isabelle.eclipse.core.app;

import isabelle.Session;

public interface IIsabelleSessionListener {

	public void systemInit();
	
	public void sessionInit(Session session);
	
	public void sessionShutdown(Session session);
	
}
