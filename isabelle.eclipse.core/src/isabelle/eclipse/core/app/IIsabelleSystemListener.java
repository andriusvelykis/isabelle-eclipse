package isabelle.eclipse.core.app;

import isabelle.scala.IsabelleSystemFacade;

public interface IIsabelleSystemListener {

	public void systemInit(IsabelleSystemFacade system);
	
	public void systemShutdown(IsabelleSystemFacade system);
	
}
