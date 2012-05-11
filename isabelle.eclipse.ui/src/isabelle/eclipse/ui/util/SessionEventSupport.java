package isabelle.eclipse.ui.util;

import isabelle.Session;
import isabelle.eclipse.core.IsabelleCorePlugin;
import isabelle.eclipse.core.app.IIsabelleSessionListener;
import isabelle.eclipse.core.app.Isabelle;
import isabelle.scala.SessionActor;
import isabelle.scala.SessionEventType;
import isabelle.scala.SessionUtil;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Assert;

/**
 * A generalisation of attaching {@link SessionActor}s to {@link Session}s when
 * they become available. The class adds listeners to Isabelle core system, and
 * attaches the actors to appropriate event buses when a session becomes
 * available. They are removed if the session is closed.
 * <p>
 * Implementors must instantiate abstract method
 * {@link #createSessionActor(Session)} to provide the actor. The actor
 * listeners should correspond to the event types given in the constructor.
 * </p>
 * 
 * @author Andrius Velykis
 */
public abstract class SessionEventSupport {

	private final EnumSet<SessionEventType> eventTypes;
	private final IIsabelleSessionListener appListener;
	
	private final Map<Session, SessionActor> sessionActors = new HashMap<Session, SessionActor>();
	
	public SessionEventSupport(Set<? extends SessionEventType> eventTypes) {
		
		this.eventTypes = EnumSet.noneOf(SessionEventType.class);
		this.eventTypes.addAll(eventTypes);
		
		this.appListener = new IIsabelleSessionListener() {

			@Override
			public void systemInit() {}

			@Override
			public void sessionInit(Session session) {
				initSession(session);
			}
			
			@Override
			public void sessionShutdown(Session session) {
				disposeSession(session);
			}
		};
		
		Isabelle isabelle = IsabelleCorePlugin.getIsabelle();
		isabelle.addSessionListener(appListener);
		
		Session existingSession = isabelle.getSession();
		if (existingSession != null) {
			initSession(existingSession);
		}
	}

	private void initSession(Session session) {
		
		Assert.isLegal(!sessionActors.containsKey(session));
		
		SessionActor sessionActor = createSessionActor(session);
		sessionActors.put(session, sessionActor);
		
		for (SessionEventType eventType : eventTypes) {
			SessionUtil.addSessionEventActor(session, eventType, sessionActor);
		}
		
		sessionInit(session);
	}
	
	protected abstract SessionActor createSessionActor(Session session);
	
	private void disposeSession(Session session) {

		sessionShutdown(session);
		
		SessionActor sessionActor = sessionActors.get(session);
		
		if (sessionActor != null) {
			for (SessionEventType eventType : eventTypes) {
				SessionUtil.removeSessionEventActor(session, eventType, sessionActor);
			}
			sessionActors.remove(session);
		}
	}
	
	protected void sessionInit(Session session) {
		// do nothing by default
	}
	
	protected void sessionShutdown(Session session) {
		// do nothing by default
	}
	
	public void dispose() {
		
		Isabelle isabelle = IsabelleCorePlugin.getIsabelle();
		isabelle.removeSessionListener(appListener);
		
		List<Session> sessions = new ArrayList<Session>(sessionActors.keySet());
		for (Session session : sessions) {
			disposeSession(session);
		}
	}
	
}
