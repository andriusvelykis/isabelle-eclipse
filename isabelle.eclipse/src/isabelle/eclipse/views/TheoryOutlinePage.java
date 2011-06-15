package isabelle.eclipse.views;

import java.util.List;

import isabelle.eclipse.IsabelleEclipseImages;
import isabelle.eclipse.editors.TheoryEditor;
import isabelle.scala.SessionFacade;
import isabelle.scala.SnapshotFacade;
import isabelle.scala.TheoryNode;

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
	
	public TheoryOutlinePage(TheoryEditor editor) {
		super();
		this.editor = editor;
	}

	public void reload() {
		
		Object input = new Object();
		
//		SessionFacade session = editor.getIsabelleSession();
//		if (session != null) {
//			String theoryName = editor.getTheoryName();
//			
//			String text = "";
//			IDocument document = editor.getDocument();
//			if (document != null) {
//				text = document.get();
//			}
//			
//			List<TheoryNode> nodes = TheoryNode.getTree(session.getSession(), theoryName, text, 0);
//			input = nodes.toArray();
//		}
		
		SnapshotFacade snapshot = editor.getSnapshot();
		if (snapshot != null) {
			TheoryNode root = TheoryNode.getRawTree(snapshot.getSnapshot(), 0);
			input = new Object[] { (Object) root };
		}
		
		TreeViewer viewer = getTreeViewer();
		if (viewer != null) {
			viewer.setInput(input);
		}
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
			return IsabelleEclipseImages.getImage(IsabelleEclipseImages.IMG_OUTLINE_ITEM);
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
