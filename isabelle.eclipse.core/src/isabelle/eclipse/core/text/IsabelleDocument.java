package isabelle.eclipse.core.text;

import isabelle.Symbol;
import isabelle.eclipse.core.IsabelleCorePlugin;
import isabelle.eclipse.core.app.Isabelle;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;


public class IsabelleDocument extends Document {

	private final IDocument baseDocument;
	private boolean syncingFromBase = false;
	private boolean syncingToBase = false;
	
	public IsabelleDocument(IDocument baseDocument) {
		this.baseDocument = baseDocument;
		
		this.baseDocument.addDocumentListener(new IDocumentListener() {
			
			@Override
			public void documentChanged(DocumentEvent event) {
				if (syncingToBase) {
					return;
				}
				
//				System.out.println("Sync document - changed");
				syncFromBase();
			}
			
			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
				if (syncingToBase) {
					return;
				}
				
//				System.out.println("Sync document - about to be changed");
			}
		});
		
		this.addDocumentListener(new IDocumentListener() {
			
			@Override
			public void documentChanged(DocumentEvent event) {
				if (syncingFromBase) {
					return;
				}
				
				syncToBase();
			}
			
			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {}
		});
		
		syncFromBase();
	}
	
	public IDocument getBaseDocument() {
		return this.baseDocument;
	}
	
	private void syncFromBase() {
		
//		System.out.println("Sync from base");
		String text = baseDocument.get();
		
		Isabelle isabelle = IsabelleCorePlugin.getIsabelle();
		if (isabelle.isInit()) {
//			System.out.println("Sync from base Isabelle");
			text = Symbol.decode(text);
		}
		
		syncingFromBase = true;
		this.set(text);
		syncingFromBase = false;
	}
	
	public void syncToBase() {
//		System.out.println("Sync to base");
		
		String text = get();
		
		Isabelle isabelle = IsabelleCorePlugin.getIsabelle();
		if (isabelle.isInit()) {
//			System.out.println("Sync to base Isabelle");
			text = Symbol.encode(text);
		}
		
		syncingToBase = true;
		this.baseDocument.set(text);
		syncingToBase = false;
	}
	
}
