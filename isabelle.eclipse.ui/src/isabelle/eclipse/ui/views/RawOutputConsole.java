package isabelle.eclipse.ui.views;

import java.io.IOException;
import isabelle.Isabelle_Process.Output;
import isabelle.Event_Bus;
import isabelle.Session;
import isabelle.XML;
import isabelle.eclipse.core.util.SafeSessionActor;
import isabelle.eclipse.core.util.SessionEventSupport;
import isabelle.eclipse.ui.IsabelleUIPlugin;
import isabelle.scala.ISessionRawMessageListener;
import isabelle.scala.ScalaCollections;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;

import scala.actors.Actor;


public class RawOutputConsole extends MessageConsole {
	
	private final SessionEventSupport<?> sessionEvents;

	private IOConsoleOutputStream consoleStream;
	
	public RawOutputConsole(String name, ImageDescriptor imageDescriptor) {
		super(name, imageDescriptor, true);
		
		sessionEvents = new SessionEventSupport<Output>() {
			
			@Override
			public Actor sessionActor() {
				return (Actor) new SafeSessionActor().rawMessages(new ISessionRawMessageListener() {
					
					@Override
					public void handleMessage(Output result) {
						outputMessage(result);
					}
				}).getActor();
			}
			
			@Override
			public scala.collection.immutable.List<Event_Bus<Output>> sessionEvents0(Session session) {
				return ScalaCollections.singletonList(session.raw_output_messages());
			}
		};
		sessionEvents.init();
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
	}
	
	@Override
	protected void dispose() {
		
		sessionEvents.dispose();
		
		if (consoleStream != null) {
			try {
				consoleStream.close();
			} catch (IOException e) {
				IsabelleUIPlugin.log("Unable to close raw output console", e);
			} finally {
				consoleStream = null;
			}
			
		}
		
		super.dispose();
	}
	
	private void outputMessage(Output result) {

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
			IsabelleUIPlugin.log("Problems writing to raw output console", e);
		}
		
	}


}
