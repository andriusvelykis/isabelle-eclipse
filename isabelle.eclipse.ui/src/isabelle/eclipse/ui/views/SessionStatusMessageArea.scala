package isabelle.eclipse.ui.views

import scala.actors.Actor
import scala.util.Try

import org.eclipse.jface.layout.{GridDataFactory, GridLayoutFactory}
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.{FillLayout, GridData}
import org.eclipse.swt.widgets.{Composite, Control, Event, Label, Link, Listener}
import org.eclipse.ui.{ISharedImages, PlatformUI}
import org.eclipse.ui.handlers.IHandlerService
import org.eclipse.ui.services.IServiceLocator

import isabelle.Session
import isabelle.eclipse.core.IsabelleCore
import isabelle.eclipse.core.util.SessionEvents
import isabelle.eclipse.ui.internal.IsabelleUIPlugin.{error, log}
import isabelle.eclipse.ui.util.SWTUtil
import isabelle.eclipse.ui.util.SWTUtil.Disposable


/**
 * A status-like area that displays a warning when Isabelle prover is not running.
 * 
 * Can be used as a header to view part to indicate no data without prover.
 * 
 * @author Andrius Velykis
 */
class SessionStatusMessageArea(services: IServiceLocator) extends SessionEvents {

  private var main: Composite = _
  
  def createControl(parent: Composite) {
    
    main = new Composite(parent, SWT.NONE)
    main.setLayout(GridLayoutFactory.fillDefaults.numColumns(2).spacing(0, 0).create)

    val tooltip = "Isabelle prover is not running: configure and start it " +
                  "in Isabelle launch configurations"
    
    val iconLabel = new Label(main, SWT.NONE)
    val warnImg = PlatformUI.getWorkbench.getSharedImages.getImage(ISharedImages.IMG_OBJS_WARN_TSK)
    iconLabel.setImage(warnImg)
    iconLabel.setToolTipText(tooltip)
    iconLabel.setLayoutData(GridDataFactory.swtDefaults.create)
    
    val textLabel = new Link(main, SWT.WRAP)
    // display a link in the message to configure prover
    textLabel.setText("Isabelle prover is <a>not running</a>")
    textLabel.setToolTipText(tooltip)
    textLabel.setLayoutData(GridDataFactory.fillDefaults.grab(true, false).create)

    // launch Isabelle prover configuration dialog when link is selected
    textLabel.addListener(SWT.Selection, new Listener() {
      override def handleEvent(event: Event) = handlerService match {
        case Some(handler) => {

          val exec = Try(handler.executeCommand(
            "isabelle.eclipse.launch.openIsabelleConfigurations", null))

          // log if failed
          exec.failed foreach (ex => log(error(Some(ex))))
        }

        case _ =>
      }
    })
    
    val separator = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL)
    separator.setLayoutData(GridDataFactory.fillDefaults.span(2, 1).grab(true, false).create)
    
    // hook dispose listener
    main onDispose dispose()
    
    initSessionEvents()
  }

  /**
   * Resolves the command handler service
   */
  private def handlerService: Option[IHandlerService] =
    services.getService(classOf[IHandlerService]) match {
      case s: IHandlerService => Some(s)
      case _ => None
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
  
  def wrapPart(parent: Composite, services: IServiceLocator): (Control, Composite) = {

    val statusArea = new SessionStatusMessageArea(services)
    
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
