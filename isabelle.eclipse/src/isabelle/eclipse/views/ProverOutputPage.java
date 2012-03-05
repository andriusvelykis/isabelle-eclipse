package isabelle.eclipse.views;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import isabelle.Command;
import isabelle.Document.Snapshot;
import isabelle.Markup;
import isabelle.Session;
import isabelle.XML.Tree;
import isabelle.eclipse.IsabelleEclipsePlugin;
import isabelle.eclipse.editors.DocumentModel;
import isabelle.eclipse.editors.TheoryEditor;
import isabelle.scala.SnapshotUtil;

import org.apache.commons.lang.ObjectUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.Page;
import org.osgi.framework.Bundle;

import scala.Option;

public class ProverOutputPage extends Page {

	/*
	 * TODO implement for IE: http://www.quirksmode.org/dom/range_intro.html, http://help.dottoro.com/ljumcfud.php
	 */
	private static final String SELECT_ALL_JAVASCRIPT = "window.getSelection().selectAllChildren(document.body);";
	
	private final TheoryEditor editor;
	
	private Composite mainComposite;
	private Browser outputArea;
	private Command currentCommand;
	
	private ISelectionChangedListener editorListener;
	
	private boolean showTrace = false;
	private boolean followSelection = true;
	private int lastEditorCaretOffset = 0;
	
	private Job updateJob = new UpdateOutputJob(0, Collections.<Command>emptySet());
	public ProverOutputPage(TheoryEditor editor) {
		this.editor = editor;
	}

	@Override
	public void createControl(Composite parent) {
		mainComposite = new Composite(parent, SWT.NULL);
        mainComposite.setLayout(new FillLayout());
        
        outputArea = new Browser(mainComposite, SWT.NONE);
        outputArea.setText("Initialised output view");
	}

	@Override
	public Control getControl() {
		return mainComposite;
	}

	@Override
	public void init(IPageSite pageSite) {
		super.init(pageSite);
		
		editorListener = new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (followSelection) {
					// the reaction to selection change is delayed
//					Session session = IsabelleCorePlugin.getIsabelle().getSession();
//					long delay = session != null ? session.input_delay().ms() : 300;
					long delay = 0L;
					lastEditorCaretOffset = editor.getCaretPosition();
					updateOutput(delay, lastEditorCaretOffset, Collections.<Command>emptySet());
				}
			}
		};
		
		ISelectionProvider selectionProvider = editor.getSelectionProvider();
		if (selectionProvider instanceof IPostSelectionProvider) {
			((IPostSelectionProvider) selectionProvider).addPostSelectionChangedListener(editorListener);
		} else {
			selectionProvider.addSelectionChangedListener(editorListener);
		}
		
	}
	
	private void updateOutput(long delay, int offset, Set<Command> restriction) {
		Job updateJob = new UpdateOutputJob(offset, restriction);
		
		// cancel the previous job
		this.updateJob.cancel();
		this.updateJob = updateJob;
		
		this.updateJob.schedule(delay);
	}
	
	private boolean updateCommand(int offset) {
		
		if (outputArea == null) {
			// not initialised yet
			return false;
		}
		
		Command selectedCommand = getCommandAtOffset(offset);
		if (ObjectUtils.equals(this.currentCommand, selectedCommand)) {
			return false;
		}
		
		this.currentCommand = selectedCommand;
		return true;
	}
	
	private Command getCommandAtOffset(int offset) {
		
		DocumentModel isabelleModel = editor.getIsabelleModel();
		if (isabelleModel == null) {
			return null;
		}
		
		Snapshot snapshot = isabelleModel.getSnapshot();
		
		Option<Command> cmd = snapshot.node().proper_command_at(offset);
		return cmd.isDefined() ? cmd.get() : null;
	}

	private String renderOutput(int offset, Set<Command> restriction, IProgressMonitor monitor) {
		long start = System.currentTimeMillis();
		System.out.println("Starting update");
		// TODO: do not redo the same command?
		updateCommand(offset);
		
		System.out.println("Finished update: " + (System.currentTimeMillis() - start));
		
		if (currentCommand == null) {
			System.out.println("No command");
			return null;
		}
		
		boolean updateCommand = restriction.isEmpty() || restriction.contains(currentCommand);
		if (!updateCommand) {
			System.out.println("Ignored command " + currentCommand.name());
			return null;
		}
		
		// TODO do not output when invisible?
		// FIXME handle "sendback" in output_dockable.scala
		
		// get all command results except tracing
		DocumentModel isabelleModel = editor.getIsabelleModel();
		if (isabelleModel == null) {
			System.out.println("Isabelle model not available");
			return null;
		}
		
		Snapshot snapshot = isabelleModel.getSnapshot();
		String[] excludeFilter = showTrace ? new String[0] : new String[] { Markup.TRACING() };
		List<Tree> commandResults = SnapshotUtil.getCommandResults(snapshot,
				currentCommand, new String[0], excludeFilter);

		System.out.println("Got command results: " + (System.currentTimeMillis() - start));
		
		String htmlPage = PrettyHtml.renderHtmlPage(commandResults, getCssPaths(), "", "IsabelleText", 12);

		System.out.println("Done rendering: " + (System.currentTimeMillis() - start));
		
		return htmlPage;
	}
	
	private List<String> getCssPaths() {
		List<String> cssPaths = new ArrayList<String>();
		Bundle bundle = Platform.getBundle(IsabelleEclipsePlugin.PLUGIN_ID);
		addResourcePath(cssPaths, bundle, "etc/isabelle.css");
		addResourcePath(cssPaths, bundle, "etc/isabelle-jedit.css");
		return cssPaths;
	}
	
	private void addResourcePath(List<String> paths, Bundle bundle, String pathInBundle) {
		URL fileURL = bundle.getEntry(pathInBundle);
		if (fileURL == null) {
			IsabelleEclipsePlugin.log("Unable to locate resource " + pathInBundle, null);
			return;
		}
		
		try {
			URL fullURL = FileLocator.resolve(fileURL);
			String path = fullURL.toString();
			paths.add(path);
		} catch (IOException e) {
			IsabelleEclipsePlugin.log("Unable to locate resource " + pathInBundle, e);
		}
	}
	
	private void setContent(final String htmlPage) {
		if (outputArea != null) {

			// set the input in the UI thread
			outputArea.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					outputArea.setText(htmlPage);
				}
			});
		}
	}
	
	@Override
	public void dispose() {
		
		if (editorListener != null) {
			
			ISelectionProvider selectionProvider = editor.getSelectionProvider();
			if (selectionProvider instanceof IPostSelectionProvider) {
				((IPostSelectionProvider) selectionProvider).removePostSelectionChangedListener(editorListener);
			}
			
			selectionProvider.removeSelectionChangedListener(editorListener);
		}
		
		outputArea = null;
		
		super.dispose();
	}

	@Override
	public void setFocus() {
		
	}
	
	public void selectAllText() {
		if (outputArea != null) {
			outputArea.execute(SELECT_ALL_JAVASCRIPT);
		}
	}
	
	public void copySelectionToClipboard() {
		// do nothing at the moment - allow browser copy facilities to work
	}
	
	private class UpdateOutputJob extends Job {

		private final int offset;
		private final Set<Command> restriction;
		
		public UpdateOutputJob(int offset, Set<Command> restriction) {
			super("Updating prover output");
			this.offset = offset;
			this.restriction = new HashSet<Command>(restriction);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			
			String result = renderOutput(offset, restriction, monitor);
			
			// check if cancelled - then do not set the outline
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			
			if (result != null) {
				setContent(result);
			}
			
			return Status.OK_STATUS;
		}
	}

}
