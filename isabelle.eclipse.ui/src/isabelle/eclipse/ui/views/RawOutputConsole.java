package isabelle.eclipse.ui.views;

import java.io.IOException;
import java.util.EnumSet;
import isabelle.Isabelle_Process.Result;
import isabelle.Session;
import isabelle.XML;
import isabelle.eclipse.core.util.SafeSessionActor;
import isabelle.eclipse.core.util.SessionEventSupport;
import isabelle.eclipse.ui.IsabelleUIPlugin;
import isabelle.scala.ISessionRawMessageListener;
import isabelle.scala.SessionActor;
import isabelle.scala.SessionEventType;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;

public class RawOutputConsole extends MessageConsole {
	
	private final SessionEventSupport sessionEvents;

	private IOConsoleOutputStream consoleStream;
	
	public RawOutputConsole(String name, ImageDescriptor imageDescriptor) {
		super(name, imageDescriptor, true);
		
		sessionEvents = new SessionEventSupport(EnumSet.of(SessionEventType.RAW_MESSAGES)) {
			
			@Override
			protected SessionActor createSessionActor(Session session) {
				return new SafeSessionActor().rawMessages(new ISessionRawMessageListener() {
					
					@Override
					public void handleMessage(Result result) {
						outputMessage(result);
					}
				});
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
			IsabelleUIPlugin.log("Problems writing to raw output console", e);
		}
		
	}


}
