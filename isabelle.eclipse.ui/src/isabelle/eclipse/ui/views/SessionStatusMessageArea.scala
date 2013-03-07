package isabelle.eclipse.ui.views

import scala.actors.Actor

import org.eclipse.jface.layout.{GridDataFactory, GridLayoutFactory}
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.{FillLayout, GridData}
import org.eclipse.swt.widgets.{Composite, Control, Label}
import org.eclipse.ui.{ISharedImages, PlatformUI}

import isabelle.Session
import isabelle.eclipse.core.IsabelleCore
import isabelle.eclipse.core.util.SessionEvents
import isabelle.eclipse.ui.util.SWTUtil
import isabelle.eclipse.ui.util.SWTUtil.Disposable


/**
 * A status-like area that displays a warning when Isabelle prover is not running.
 * 
 * Can be used as a header to view part to indicate no data without prover.
 * 
 * @author Andrius Velykis
 */
class SessionStatusMessageArea extends SessionEvents {

  private var main: Composite = _
  
  def createControl(parent: Composite) {
    
    main = new Composite(parent, SWT.NONE)
    main.setLayout(GridLayoutFactory.fillDefaults.numColumns(2).spacing(0, 0).create)
    
    val tooltip = "Isabelle prover is not running: start it as External Tool"
    
    val iconLabel = new Label(main, SWT.NONE)
    val warnImg = PlatformUI.getWorkbench.getSharedImages.getImage(ISharedImages.IMG_OBJS_WARN_TSK)
    iconLabel.setImage(warnImg)
    iconLabel.setToolTipText(tooltip)
    iconLabel.setLayoutData(GridDataFactory.swtDefaults.create)
    
    val textLabel = new Label(main, SWT.WRAP)
    textLabel.setText("Isabelle prover is not running")
    textLabel.setToolTipText(tooltip)
    textLabel.setLayoutData(GridDataFactory.fillDefaults.grab(true, false).create)
    
    val separator = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL)
    separator.setLayoutData(GridDataFactory.fillDefaults.span(2, 1).grab(true, false).create)
    
    // hook dispose listener
    main onDispose dispose()
    
    initSessionEvents()
  }
  
  def getControl(): Control = main
  
  def update() = updateSessionStatus()
  
  
  // the actor to react to session events (not used)
  override protected val sessionActor: Actor = null

  // no session events to subscribe - we are only interested in init/shutdown sequences here
  override protected def sessionEvents(session: Session) = Nil
  
  override protected def sessionInit(session: Session) = updateSessionStatusInUI()
  
  override protected def sessionShutdown(session: Session) = updateSessionStatusInUI()
  
  private def updateSessionStatusInUI() {
    val control = Option(getControl) filterNot (_.isDisposed)
    
    control foreach { c =>
      SWTUtil.asyncExec(Some(c.getDisplay)){ updateSessionStatus() }
    }
  }
  
  private def updateSessionStatus(init: Boolean = false) {
    
    // just hide/show the whole area
    val running = IsabelleCore.isabelle.isRunning
    showControl(main, !running, init)
  }
  
  private def showControl(control: Control, show: Boolean, init: Boolean = false) =
    if (!control.isDisposed) {
      val layoutData = control.getLayoutData.asInstanceOf[GridData]
      val hide = !show

      if (hide != layoutData.exclude) {
        layoutData.exclude = hide
        control.setVisible(!hide)
        if (!init && !control.getParent.isDisposed) {
          control.getParent.layout(false)
        }
      }
    }
  
  def dispose() {
    disposeSessionEvents()
  }
  
}


object SessionStatusMessageArea {
  
  def wrapPart(parent: Composite): (Control, Composite) = {

    val statusArea = new SessionStatusMessageArea
    
    val main = new Composite(parent, SWT.NONE)
    main.setLayout(GridLayoutFactory.fillDefaults.create)
    
    statusArea.createControl(main)
    statusArea.getControl.setLayoutData(GridDataFactory.fillDefaults.grab(true, false).create)
    
    val contentComposite = new Composite(main, SWT.NONE)
    contentComposite.setLayout(new FillLayout)
    contentComposite.setLayoutData(GridDataFactory.fillDefaults.grab(true, true).create)
    
    statusArea.update()

    (main, contentComposite)
  }
  
}
