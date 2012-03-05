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
import isabelle.Thy_Load;
import isabelle.scala.ISessionPhaseListener;
import isabelle.scala.ScalaCollections;
import isabelle.scala.SessionActor;
import isabelle.scala.SessionEventType;
import isabelle.scala.SessionUtil;


public class Isabelle {

	public static final Session$Failed$ SESSION_FAILED = Session$Failed$.MODULE$;
	public static final Session$Ready$ SESSION_READY = Session$Ready$.MODULE$;
	public static final Session$Shutdown$ SESSION_SHUTDOWN = Session$Shutdown$.MODULE$;
	
	private Session session;
	
	private final ListenerList sessionListeners = new ListenerList();
	
	private final SessionActor sessionManager;
	
	private boolean systemInit = false;
	
	public Isabelle() {
		this.sessionManager = new SessionActor().phaseChanged(new ISessionPhaseListener() {
			
			@Override
			public void phaseChanged(Phase phase) {
				
				if (phase == SESSION_READY) {
					fireSessionInitialised(session);
				} else if (phase == SESSION_SHUTDOWN) {
					fireSessionRemoved(session);
				}
			}
		});
	}
	
	public boolean isInit() {
		return systemInit;
	}
	
	public boolean isRunning() {
		
		if (session == null) {
			return false;
		}
		
		return session.phase() == SESSION_READY;
	}
	
	public Session start(String isabellePath, String logic) {
		
		// TODO check paths for the same system?
		if (isInit()) {
			throw new IllegalStateException("Isabelle already initialised!");
		}
		
		Isabelle_System.init(isabellePath);
//		Isabelle_System.install_fonts();
		
		systemInit = true;
		
//	    Isabelle.setup_tooltips()
//	    Isabelle_System.init()
//	    Isabelle_System.install_fonts()
//	    Isabelle.session = new Session(Isabelle.thy_load)
//	    SyntaxUtilities.setStyleExtender(new Token_Markup.Style_Extender)
//	    if (ModeProvider.instance.isInstanceOf[ModeProvider])
//	      ModeProvider.instance = new Token_Markup.Mode_Provider(ModeProvider.instance)
//	    Isabelle.session.phase_changed += session_manager
		
		fireSystemInit();
		
		session = new Session(new Thy_Load());
		
		List<String> sessionArgs = Arrays.asList("-mxsymbols", /*"-mno_brackets", "-mno_type_brackets",*/ logic ); 
		
		session.start(ScalaCollections.toScalaList(sessionArgs));
		
		// start listening for session phase changes
		SessionUtil.addSessionEventActor(session, SessionEventType.PHASE, sessionManager);
		
		return session;
	}
	
	public void stop() {
		
		if (session != null) {
			
			SessionUtil.removeSessionEventActor(session, SessionEventType.PHASE, sessionManager);
			
			session.stop();
			fireSessionRemoved(session);
		}
		
		session = null;
		systemInit = false;
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
