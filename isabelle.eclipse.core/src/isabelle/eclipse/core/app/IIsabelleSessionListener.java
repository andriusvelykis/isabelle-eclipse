package isabelle.eclipse.core.app;

import isabelle.scala.SessionFacade;

public interface IIsabelleSessionListener {

	public void sessionInit(SessionFacade session);
	
	public void sessionShutdown(SessionFacade session);
	
}
