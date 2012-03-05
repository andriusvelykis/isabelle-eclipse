package isabelle.eclipse.views;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import isabelle.Command;
import isabelle.Markup;
import isabelle.XML.Tree;
import isabelle.eclipse.editors.TheoryEditor;

import org.apache.commons.lang.ObjectUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.Page;
import org.osgi.framework.Bundle;


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
				updateOutput();
			}
		};
		
		ISelectionProvider selectionProvider = editor.getSelectionProvider();
		if (selectionProvider instanceof IPostSelectionProvider) {
			((IPostSelectionProvider) selectionProvider).addPostSelectionChangedListener(editorListener);
		} else {
			selectionProvider.addSelectionChangedListener(editorListener);
		}
		
	}
	
	private boolean updateCommand() {
		
		if (outputArea == null) {
			// not initialised yet
			return false;
		}
		
		Command selectedCommand = editor.getSelectedCommand();
		if (ObjectUtils.equals(this.currentCommand, selectedCommand)) {
			return false;
		}
		
		this.currentCommand = selectedCommand;
		return true;
	}

	private void updateOutput() {
		long start = System.currentTimeMillis();
		System.out.println("Starting update");
		// TODO: do not redo the same command?
		updateCommand();
		
		System.out.println("Finished update: " + (System.currentTimeMillis() - start));
		
		if (currentCommand == null) {
			System.out.println("No command");
			return;
		}
		
		
		// TODO do not output when invisible?
		
		// get all command results except tracing
		List<Tree> commandResults = editor.getSnapshot().commandResults(
				currentCommand, new String[0], new String[] { Markup.TRACING() });

		System.out.println("Got command results: " + (System.currentTimeMillis() - start));
		
		String htmlPage = PrettyHtml.renderHtmlPage(commandResults, getCssPaths(), "", "IsabelleText", 12);

		System.out.println("Done rendering: " + (System.currentTimeMillis() - start));
		
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

}
