package isabelle.eclipse.views;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import isabelle.Isabelle_Process.Result;
import isabelle.Session;
import isabelle.XML;
import isabelle.eclipse.IsabelleEclipsePlugin;
import isabelle.eclipse.core.IsabelleCorePlugin;
import isabelle.eclipse.core.app.IIsabelleSessionListener;
import isabelle.eclipse.core.app.IIsabelleSystemListener;
import isabelle.eclipse.core.app.Isabelle;
import isabelle.scala.ISessionRawMessageListener;
import isabelle.scala.IsabelleSystemFacade;
import isabelle.scala.SessionActor;
import isabelle.scala.SessionFacade;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;

public class RawOutputConsole extends MessageConsole {

	private final IIsabelleSessionListener appListener;
	
	private final Map<SessionFacade, SessionActor> sessionActors = new HashMap<SessionFacade, SessionActor>();
	
	private IOConsoleOutputStream consoleStream;
	
	public RawOutputConsole(String name, ImageDescriptor imageDescriptor) {
		super(name, imageDescriptor, true);
		
		this.appListener = new IIsabelleSessionListener() {

			@Override
			public void sessionInit(SessionFacade session) {
				initSession(session);
			}
			
			@Override
			public void sessionShutdown(SessionFacade session) {
				disposeSession(session);
			}
			
		};
		
	}

	@Override
	protected void init() {
		super.init();

		if (consoleStream == null) {
			consoleStream = this.newOutputStream();
		}
		
		try {
			consoleStream.write("Starting Raw Output Console");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Isabelle isabelle = IsabelleCorePlugin.getIsabelle();
		isabelle.addSessionListener(appListener);
		
		initSession(isabelle.getSession());
		
	}
	
	private void initSession(SessionFacade session) {
		System.out.println("Init session console: " + session);
		if (session == null) {
			return;
		}
		
		SessionActor sessionActor = sessionActors.get(session);
		
		if (sessionActor == null) {
			sessionActor = new SessionActor().rawMessages(new ISessionRawMessageListener() {
				
				@Override
				public void handleMessage(ResultFacade result) {
					outputMessage(result);
				}
			});
			
			sessionActors.put(session, sessionActor);
		}
		
		session.addRawMessagesActor(sessionActor);
	}
	
	private void disposeSession(SessionFacade session) {
		
		if (session == null) {
			return;
		}
		
		SessionActor sessionActor = sessionActors.get(session);
		
		if (sessionActor != null) {
			session.removeRawMessagesActor(sessionActor);
			sessionActors.remove(session);
		}
	}
	
	
	@Override
	protected void dispose() {
		
		Isabelle isabelle = IsabelleCorePlugin.getIsabelle();
		isabelle.removeSessionListener(appListener);
		
		List<SessionFacade> sessions = new ArrayList<SessionFacade>(sessionActors.keySet());
		for (SessionFacade session : sessions) {
			disposeSession(session);
		}
		
		if (consoleStream != null) {
			try {
				consoleStream.close();
			} catch (IOException e) {
				IsabelleEclipsePlugin.log("Unable to close raw output console", e);
			} finally {
				consoleStream = null;
			}
			
		}
		
		super.dispose();
	}
	
	private void outputMessage(Result result) {

		if (consoleStream == null) {
			return;
		}
		
		if (!result.is_stdout()) {
			return;
		}
		
		String output = XML.content(result.message()).mkString();
		
		try {
			consoleStream.write(output);
		} catch (IOException e) {
			IsabelleEclipsePlugin.log("Problems writing to raw output console", e);
		}
		
	}


}
