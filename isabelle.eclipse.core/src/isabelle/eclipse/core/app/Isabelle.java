package isabelle.eclipse.core.app;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.ListenerList;

import isabelle.Isabelle_System;
import isabelle.Session;
import isabelle.Session.Phase;
import isabelle.Session$Failed$;
import isabelle.Session$Ready$;
import isabelle.Session$Shutdown$;


public class Isabelle {

	public static final Session$Failed$ SESSION_FAILED = Session$Failed$.MODULE$;
	public static final Session$Ready$ SESSION_READY = Session$Ready$.MODULE$;
	public static final Session$Shutdown$ SESSION_SHUTDOWN = Session$Shutdown$.MODULE$;
	
	private Session session;
	
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
		
		
		session = new SessionFacade(system.getSystem());
		fireSystemInit();
		
		
		String[] sessionArgs = {"-mxsymbols", /*"-mno_brackets", "-mno_type_brackets",*/ logic }; 
		
		session.start(10000, sessionArgs);
	}
	
	public void stop() {
		
		if (session != null) {
			session.stop();
			fireSessionRemoved(session);
		}
		
		session = null;
		
		system = null;
	}
	
	public IsabelleSystemFacade getSystem() {
		return system;
	}
	
	public Session getSession() {
		return session;
	}
	
	public void addSessionListener(IIsabelleSessionListener listener) {
		sessionListeners.add(listener);
	}
	
	public void removeSessionListener(IIsabelleSessionListener listener) {
		sessionListeners.remove(listener);
	}
	
	private void fireSystemInit() {
		for (Object listener : sessionListeners.getListeners()) {
			((IIsabelleSessionListener) listener).systemInit();
		}
	}
	
	private void fireSessionInitialised(Session session) {
		for (Object listener : sessionListeners.getListeners()) {
			((IIsabelleSessionListener) listener).sessionInit(session);
		}
	}
	
	private void fireSessionRemoved(Session session) {
		for (Object listener : sessionListeners.getListeners()) {
			((IIsabelleSessionListener) listener).sessionShutdown(session);
		}
	}

}
