package isabelle.eclipse.launch.tabs;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;

public class SingleCheckedListener implements ICheckStateListener {

	private final CheckboxTableViewer viewer;
	
	public SingleCheckedListener(CheckboxTableViewer viewer) {
		this.viewer = viewer;
	}

	@Override
	public void checkStateChanged(CheckStateChangedEvent event) {
		viewer.setCheckedElements(new Object[] { event.getElement() });
	}

}
