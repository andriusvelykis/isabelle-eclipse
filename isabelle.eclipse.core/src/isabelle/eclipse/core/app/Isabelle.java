package isabelle.eclipse.core.app;

import org.eclipse.core.runtime.ListenerList;

import isabelle.scala.IsabelleSystemFacade;
import isabelle.scala.SessionFacade;
import isabelle.Session.Phase;
import isabelle.Session$Failed$;
import isabelle.Session$Ready$;
import isabelle.Session$Shutdown$;


public class Isabelle {

	private IsabelleSystemFacade system;
	private String path;
	public static final Session$Failed$ SESSION_FAILED = Session$Failed$.MODULE$;
	public static final Session$Ready$ SESSION_READY = Session$Ready$.MODULE$;
	public static final Session$Shutdown$ SESSION_SHUTDOWN = Session$Shutdown$.MODULE$;
	
	private SessionFacade session;
	
	private final ListenerList systemListeners = new ListenerList();
	private final ListenerList sessionListeners = new ListenerList();
	
	public boolean isRunning() {
		
		if (session == null) {
			return false;
		}
		
		return session.phase() == SESSION_READY;
	}
	
	public void start(String isabellePath, String logic) {
		
		// TODO check paths for the same system?
		if (system != null) {
			throw new IllegalStateException("Isabelle already exists!");
		}
		
		system = new IsabelleSystemFacade(isabellePath);
		system.getSystem().install_fonts();
		
		this.path = isabellePath;
		fireSystemInit(system);
		
		session = new SessionFacade(system.getSystem());
		
		fireSessionInitialised(session);
		
		String[] sessionArgs = {"-mxsymbols", /*"-mno_brackets", "-mno_type_brackets",*/ logic }; 
		
		session.start(10000, sessionArgs);
	}
	
	public void stop() {
		
		if (session != null) {
			session.getSession().stop();
			fireSessionRemoved(session);
		}
		
		session = null;
		
		fireSystemShutdown(system);
		path = null;
		system = null;
	}
	
	public IsabelleSystemFacade getSystem() {
		return system;
	}
	
	public SessionFacade getSession() {
		return session;
	}
	
	public void addSystemListener(IIsabelleSystemListener listener) {
		systemListeners.add(listener);
	}
	
	public void removeSystemListener(IIsabelleSystemListener listener) {
		systemListeners.remove(listener);
	}
	
	private void fireSystemInit(IsabelleSystemFacade system) {
		for (Object listener : systemListeners.getListeners()) {
			((IIsabelleSystemListener) listener).systemInit(system);
		}
	}
	
	private void fireSystemShutdown(IsabelleSystemFacade system) {
		for (Object listener : systemListeners.getListeners()) {
			((IIsabelleSystemListener) listener).systemShutdown(system);
		}
	}
	
	public void addSessionListener(IIsabelleSessionListener listener) {
		sessionListeners.add(listener);
	}
	
	public void removeSessionListener(IIsabelleSessionListener listener) {
		sessionListeners.remove(listener);
	}
	
	private void fireSessionInitialised(SessionFacade session) {
		for (Object listener : sessionListeners.getListeners()) {
			((IIsabelleSessionListener) listener).sessionInit(session);
		}
	}
	
	private void fireSessionRemoved(SessionFacade session) {
		for (Object listener : sessionListeners.getListeners()) {
			((IIsabelleSessionListener) listener).sessionShutdown(session);
		}
	}

}
