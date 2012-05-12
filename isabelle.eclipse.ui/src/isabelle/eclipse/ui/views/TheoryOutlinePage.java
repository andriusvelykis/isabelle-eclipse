package isabelle.eclipse.ui.views;

import java.util.Collections;
import java.util.List;

import isabelle.eclipse.core.text.DocumentModel;
import isabelle.eclipse.ui.IsabelleImages;
import isabelle.eclipse.ui.editors.TheoryEditor;
import isabelle.scala.TheoryNode;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

public class TheoryOutlinePage extends ContentOutlinePage {

	private final TheoryEditor editor;
	
	private boolean rawTree = false;
	
	// just so that it is not null
	private Job updateJob = new OutlineParseJob();
	
	public TheoryOutlinePage(TheoryEditor editor) {
		super();
		this.editor = editor;
	}

	public void reload() {

		Job updateJob = new OutlineParseJob();
		
		// cancel the previous run, in case it is still executing
		this.updateJob.cancel();
		this.updateJob = updateJob;
		
		this.updateJob.schedule();
	}
	
	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		
		TreeViewer viewer = getTreeViewer();
		viewer.setContentProvider(new TheoryNodeContentProvider());
		viewer.setLabelProvider(new TheoryNodeLabelProvider());
		viewer.setAutoExpandLevel(2);
		reload();
	}
	
	private List<TheoryNode> parseOutline() {
		
		DocumentModel isabelleModel = editor.getIsabelleModel();
		if (isabelleModel == null) {
			// return a dummy element signaling that the prover is not available
			return getNoIsabelleNode();
		}
		
		if (rawTree) {
			return parseRawOutline(isabelleModel);
		} else {
			return parseIsabelleOutline(isabelleModel);
		}
	}
	
	private List<TheoryNode> parseIsabelleOutline(DocumentModel isabelleModel) {
		
		String text = "";
		IDocument document = editor.getDocument();
		if (document != null) {
			text = document.get();
		}
		
		return TheoryNode.getTree(isabelleModel.getSession(), isabelleModel.getName(), text);
	}
	
	private List<TheoryNode> parseRawOutline(DocumentModel isabelleModel) {
		TheoryNode root = TheoryNode.getRawTree(isabelleModel.getSnapshot());
		return Collections.singletonList(root);
	}
	
	private List<TheoryNode> getNoIsabelleNode() {
		return Collections.singletonList(new TheoryNode("<prover is not running>", 0, 0));
	}
	
	private void setInput(final List<TheoryNode> roots) {
		final TreeViewer viewer = getTreeViewer();
		if (viewer != null) {

			// set the input in the UI thread
			viewer.getControl().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					viewer.setInput(roots.toArray());
				}
			});
		}
	}
	
	private class OutlineParseJob extends Job {

		public OutlineParseJob() {
			super("Creating theory outline tree");
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			
			List<TheoryNode> result = parseOutline();
			
			// check if cancelled - then do not set the outline
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			
			setInput(result);
			
			return Status.OK_STATUS;
		}
	}
	
	private static class TheoryNodeContentProvider extends AbstractTheoryNodeContentProvider {
		
		@Override
		public Object[] getElements(Object inputElement) {
			
			if (inputElement instanceof Object[]) {
				return (Object[]) inputElement;
			}
			
			return emptyArray;
		}
	}
	
	private static abstract class AbstractTheoryNodeContentProvider implements ITreeContentProvider {

		protected final Object[] emptyArray = {};
		
		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof TheoryNode) {
				return ((TheoryNode) parentElement).getChildren().toArray();
			}
			
			return emptyArray;
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof TheoryNode) {
				TheoryNode node = (TheoryNode) element;
				TheoryNode parent = node.getParent();
				return (Object) parent;
			}
			
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof TheoryNode) {
				return ((TheoryNode) element).getChildren().size() > 0;
			}
			
			return false;
		}
		
		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// Do nothing
		}
		
	}
	
	private static class TheoryNodeLabelProvider extends BaseLabelProvider implements ILabelProvider {

		@Override
		public Image getImage(Object element) {
			return IsabelleImages.getImage(IsabelleImages.IMG_OUTLINE_ITEM);
		}

		@Override
		public String getText(Object element) {
			if (element instanceof TheoryNode) {
				return ((TheoryNode) element).name();
			}
			
			return "Unknown";
		}
		
	}

}
