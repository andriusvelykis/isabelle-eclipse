package isabelle.eclipse.ui.text;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;

/**
 * <p>
 * A listener class (adapted from Listener in AbstractReconciler) to react to document changes. It
 * re-attaches itself to the most recent IDocument within the viewer.
 * </p>
 * <p>
 * The actual listener to listen to document changes is provided via the constructor.
 * </p>
 * 
 * @author Andrius Velykis
 */
public class DocumentListenerSupport
{
  
  private ITextViewer textViewer;
  private final ITextInputListener documentChangeListener;

  private IDocument document;
  private final IDocumentListener docListener;

  public DocumentListenerSupport(IDocumentListener listener)
  {
    super();
    this.docListener = listener;

    // A listener to re-attach document listener when a document changes in a viewer.
    this.documentChangeListener = new ITextInputListener()
    {
      @Override
      public void inputDocumentChanged(IDocument oldInput, IDocument newInput)
      {
        // attach itself to the new document
        if (newInput != null) {
          newInput.addDocumentListener(docListener);
          document = newInput;
        }
      }
      
      @Override
      public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput)
      {
        if (oldInput != null) {
          oldInput.removeDocumentListener(docListener);
          if (oldInput == document) {
            document = null;
          }
        }
      }
    };
    
  }
  
  public void init(ITextViewer viewer) {
	  this.textViewer = viewer;
	  
	  // Add document change listener to the source viewer.
	  textViewer.addTextInputListener(documentChangeListener);
	  // upon dispose, detach the listener
	  textViewer.getTextWidget().addDisposeListener(new DisposeListener()
	  {
	    @Override
	    public void widgetDisposed(DisposeEvent e)
	    {
	      textViewer.removeTextInputListener(documentChangeListener);
	      textViewer = null;
	    }
	  });
	  
	  // install the document listener onto the current document
	  document = textViewer.getDocument();
	  if (document != null) {
	    document.addDocumentListener(docListener);
	  }
  }
  
  public void dispose()
  {
    if (textViewer != null) {
      textViewer.removeTextInputListener(documentChangeListener);
    }
    
    if (document != null) {
      document.removeDocumentListener(docListener);
    }
  }
  
}
