package isabelle.eclipse.views;

import java.util.List;

import isabelle.Command;
import isabelle.Markup;
import isabelle.XML.Tree;
import isabelle.eclipse.editors.TheoryEditor;
import isabelle.scala.IsabelleSystemFacade;

import org.apache.commons.lang.ObjectUtils;
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
//        outputArea.setText("<html><title>Snippet</title><body><p id='myid'>Best Friends</p><p id='myid2'>Cat and Dog</p></body></html>");
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
		
		IsabelleSystemFacade system = editor.getIsabelle();
		String css = system.tryRead(new String[] {
			"/Applications/Work/Isabelle2011.app/Contents/Resources/Isabelle2011/lib/html/isabelle.css",
			"/Applications/Work/Isabelle2011.app/Contents/Resources/Isabelle2011/contrib/jedit-4.3.2_Isabelle-6d736d983d5c/etc/isabelle-jedit.css"
		});
		
		System.out.println("Finished reading CSS: " + (System.currentTimeMillis() - start));
		
//		System.out.println("Command: " + currentCommand.toString());
		
		// TODO do not output when invisible?
		
		// get all command results except tracing
		List<Tree> commandResults = editor.getSnapshot().commandResults(
				currentCommand, new String[0], new String[] { Markup.TRACING() });

		System.out.println("Got command results: " + (System.currentTimeMillis() - start));
		
//		System.out.println("Before rendering");

		String htmlPage = PrettyHtml.renderHtmlPage(system.getSystem(), commandResults, css, "IsabelleText", 12);

		System.out.println("Done rendering: " + (System.currentTimeMillis() - start));
		
//		Node bodyNode = PrettyUtils.renderHtmlBody(commandResults, null);
//		org.w3c.dom.Document htmlDoc = PrettyUtils.renderHtml(commandResults, "", null, "", 12);
		
//		System.out.println("After rendering");
		
		// print body to String
//		String xmlString = printNode(bodyNode);
		
//		System.out.println("After printing:\n" + htmlPage);
		
		outputArea.setText(htmlPage);
		
//		System.out.println("Update output");
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
