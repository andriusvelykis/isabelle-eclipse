package isabelle.eclipse.launch.tabs

import org.eclipse.core.runtime.{IPath, IProgressMonitor, IStatus, Status}
import org.eclipse.core.runtime.jobs.{ISchedulingRule, Job}
import org.eclipse.debug.core.{ILaunchConfiguration, ILaunchConfigurationWorkingCopy}
import org.eclipse.jface.dialogs.IDialogConstants
import org.eclipse.jface.layout.{GridDataFactory, GridLayoutFactory}
import org.eclipse.jface.resource.{JFaceResources, LocalResourceManager}
import org.eclipse.jface.viewers.{
  CheckStateChangedEvent,
  CheckboxTreeViewer,
  ICheckStateListener,
  TreeViewer
}
import org.eclipse.jface.wizard.ProgressMonitorPart
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.{Composite, Control, Group}
import org.eclipse.ui.dialogs.{FilteredTree, PatternFilter}

import AccessibleUtil.addControlAccessibleListener
import ObservableUtil.NotifyPublisher
import isabelle.eclipse.core.app.IsabelleBuild
import isabelle.eclipse.launch.config.{IsabelleLaunch, IsabelleLaunchConstants}
import isabelle.eclipse.launch.config.LaunchConfigUtil.{configValue, resolvePath, setConfigValue}


/**
 * A launch configuration component to select an Isabelle session (logic) in the
 * given Isabelle directory.
 *
 * Depends on Isabelle directory selection component.
 *
 * @author Andrius Velykis
 */
class SessionSelectComponent(isaPathObservable: ObservableValue[Option[String]],
                             sessionDirsObservable: ObservableValue[Seq[String]])
    extends LaunchComponent[Option[String]] {

  def attributeName = IsabelleLaunchConstants.ATTR_SESSION
  
  private var sessionCheck = new SingleCheckStateProvider[CheckboxTreeViewer]
  private var progressMonitorPart: ProgressMonitorPart = _
  private var container: LaunchComponentContainer = _
  
  private var lastFinishedJob: Option[SessionLoadJob] = None
  private var sessionLoadJob: Option[SessionLoadJob] = None
  
  
  /**
   * Creates the controls needed to select logic for the Isabelle installation.
   */
  override def createControl(parent: Composite, container: LaunchComponentContainer) {
    
    this.container = container
    
    val group = new Group(parent, SWT.NONE)
    group.setText("&Session:")
    
    group.setLayout(GridLayoutFactory.swtDefaults.create)
    group.setLayoutData(GridDataFactory.fillDefaults.grab(true, true).create)
    group.setFont(parent.getFont)
    
    val filteredSessionsViewer = new SessionFilteredTree(group, SWT.BORDER)
    val sessionsViewer = filteredSessionsViewer.getViewer
    addControlAccessibleListener(sessionsViewer.getControl, group.getText)
    
    
    val monitorComposite = new Composite(group, SWT.NONE)
    monitorComposite.setLayout(GridLayoutFactory.fillDefaults.numColumns(2).create)
    monitorComposite.setLayoutData(GridDataFactory.fillDefaults.grab(true, false).create)
    
    progressMonitorPart = new ProgressMonitorPart(monitorComposite,
        GridLayoutFactory.fillDefaults.create, false)
    progressMonitorPart.setLayoutData(GridDataFactory.fillDefaults.grab(true, false).create)
    progressMonitorPart.setFont(parent.getFont)
    monitorComposite.setVisible(false)
    
    
    // on config change in Isabelle path, update the session selection
    // (only do after UI initialisation)
    isaPathObservable.subscribeFun(_ => sessionLocsChanged())
    // the same for session dirs change
    sessionDirsObservable.subscribeFun(_ => sessionLocsChanged())
  }
  
  private def createCheckboxTreeViewer(parent: Composite, style: Int): CheckboxTreeViewer = {
    
    val sessionsViewer = new CheckboxTreeViewer(parent, 
        SWT.CHECK | SWT.SINGLE | SWT.FULL_SELECTION | style)

    sessionsViewer.getControl.setLayoutData(GridDataFactory.fillDefaults.
      grab(true, true).hint(IDialogConstants.ENTRY_FIELD_WIDTH, 50).create)

    val resourceManager = new LocalResourceManager(
      JFaceResources.getResources, sessionsViewer.getControl)
    
    sessionsViewer.setLabelProvider(new SessionLabelProvider(resourceManager))
    sessionsViewer.setContentProvider(new ArrayTreeContentProvider)
    
    sessionCheck.initViewer(sessionsViewer)
    sessionsViewer.setCheckStateProvider(sessionCheck)
    sessionsViewer.setInput(Array())
    
    sessionsViewer.addCheckStateListener(new ICheckStateListener {
      override def checkStateChanged(event: CheckStateChangedEvent) = configModified()
    })
    
    sessionsViewer
  }
  
  
  override def initializeFrom(configuration: ILaunchConfiguration) {
    val sessionName = configValue(configuration, attributeName, "")
    reloadAvailableSessions()
    
    selectedSession = if (sessionName.isEmpty) None else Some(sessionName)
  }


  private def selectedSession: Option[String] = {
    sessionCheck.checked map (_.toString)
  }
  
  private def selectedSession_= (value: Option[String]): Unit = {
    sessionCheck.checked = value
  }
  
  private def sessionLocsChanged() = reloadAvailableSessions()
  
  private def reloadAvailableSessions() {
    
    val isaPath = isaPathObservable.value
    val moreDirs = sessionDirsObservable.value map resolvePath
    // allow only valid session dirs to avoid crashing the session lookup
    val moreDirsSafe = moreDirs filter IsabelleBuild.isSessionDir
    
    isaPath match {
      case None => {
        sessionLoadJob = None
        finishedLoadingSessions(None, None, false)
      }
      
      case Some(path) => {
        
        val newLoadJob = Some(SessionLoadJob(path, moreDirsSafe))
        if (lastFinishedJob == newLoadJob) {
          // same job, avoid reloading
          sessionLoadJob = None
        } else {
          progressMonitorPart.beginTask("Loading available sessions...", IProgressMonitor.UNKNOWN)
          progressMonitorPart.getParent.setVisible(true)
          sessionLoadJob = Some(SessionLoadJob(path, moreDirsSafe))
          sessionLoadJob.get.schedule()          
        }
      }
    }
  }
  
  private case class SessionLoadJob(isaPath: String, moreDirs: Seq[IPath])
    extends Job("Loading available sessions...") {
    
    // avoid parallel loads using the sync rule
    setRule(syncLoadRule)
    
    override protected def run(monitor: IProgressMonitor): IStatus = {
    
      val sessionLoad = IsabelleLaunch.availableSessions(isaPath, moreDirs)

      runInUI(sessionCheck.viewer.getControl) { () =>
        finishedLoadingSessions(Some(this), sessionLoad.right.toOption, true)
      }
      
      sessionLoad fold ( err => err, success => Status.OK_STATUS )
    }
  }
  
  lazy val syncLoadRule = new ISchedulingRule {
    def contains(rule: ISchedulingRule) = rule == this
    def isConflicting(rule: ISchedulingRule) = rule == this
  }

  private def runInUI(uiControl: Control)(doRun: () => Unit) {
    if (!uiControl.isDisposed) {
      uiControl.getDisplay.syncExec(new Runnable {
        override def run() = doRun()
      })
    }
  }

  private def finishedLoadingSessions(loadJob: Option[SessionLoadJob],
                                      sessionsOpt: Option[List[String]],
                                      callback: Boolean) =
    if (sessionLoadJob == loadJob && !sessionCheck.viewer.getControl.isDisposed) {
      // correct loading job and config still open
      
      val sessions = sessionsOpt getOrElse Nil
      
      val currentSelection = selectedSession
      
      // if the previously selected session is available, keep the selection
      // otherwise, reset it or select a sensible default
      val newSelection = (selectedSession, sessions) match {
        case (_, Nil) => None
        case (Some(selected), ss) if ss.contains(selected) => Some(selected)
        // if only one session available, select it
        // TODO suggest some default value, e.g. HOL?
        case (None, first :: Nil) => Some(first)
        case _ => None
      }
      
      sessionCheck.viewer.setInput(sessions.toArray)
      selectedSession = newSelection
      
      sessionLoadJob = None
      lastFinishedJob = loadJob
      progressMonitorPart.getParent.setVisible(false)
      progressMonitorPart.done()
      
      if (callback) {
        container.update()
      }
    }
  
  
  override def performApply(configuration: ILaunchConfigurationWorkingCopy) {
    setConfigValue(configuration, attributeName, selectedSession)
  }

  
  override def isValid(configuration: ILaunchConfiguration,
                       newConfig: Boolean): Option[Either[String, String]] =
    if (sessionLoadJob.isDefined) {
      // still have not finished the loading job, cannot validate
      Some(Left("Loading available Isabelle logics for selection..."))
      
    } else if (sessionCheck.viewer.getTree.getItemCount == 0) {
      Some(Left("There are no Isabelle logics available in the indicated location"))

    } else selectedSession match {

      // found selection - no errors
      case Some(session) => None

      // either urge to select for new config, or report error
      case None => if (newConfig) {
        Some(Right("Please select an Isabelle logic for the indicated location"))
      } else {
        Some(Left("Isabelle logic must be selected"))
      }
    }
  
  
  private def configModified() {
    // notify listeners
    publish(selectedSession)
  }

  /**
   * A FilteredTree with sessions checkbox tree viewer as main control
   */
  private class SessionFilteredTree(parent: Composite, treeStyle: Int)
      extends FilteredTree(parent, treeStyle, new PatternFilter(), true) {

    override protected def doCreateTreeViewer(parent: Composite, style: Int): TreeViewer =
      createCheckboxTreeViewer(parent, style)
  }
  
}
