package isabelle.eclipse.launch.tabs

import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.jobs.ISchedulingRule
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy
import org.eclipse.jface.dialogs.IDialogConstants
import org.eclipse.jface.layout.GridDataFactory
import org.eclipse.jface.layout.GridLayoutFactory
import org.eclipse.jface.resource.JFaceResources
import org.eclipse.jface.resource.LocalResourceManager
import org.eclipse.jface.viewers.CheckStateChangedEvent
import org.eclipse.jface.viewers.CheckboxTreeViewer
import org.eclipse.jface.viewers.ICheckStateListener
import org.eclipse.jface.viewers.TreeViewer
import org.eclipse.jface.wizard.ProgressMonitorPart
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Group
import org.eclipse.ui.dialogs.FilteredTree
import org.eclipse.ui.dialogs.PatternFilter

import isabelle.eclipse.core.app.IsabelleBuild
import isabelle.eclipse.core.app.IsabelleBuild.IsabellePaths
import isabelle.eclipse.launch.IsabelleLaunchPlugin
import isabelle.eclipse.launch.config.IsabelleLaunch
import isabelle.eclipse.launch.config.IsabelleLaunchConstants
import isabelle.eclipse.launch.config.LaunchConfigUtil.configValue
import isabelle.eclipse.launch.config.LaunchConfigUtil.resolvePath
import isabelle.eclipse.launch.config.LaunchConfigUtil.setConfigValue

import AccessibleUtil.addControlAccessibleListener


/**
 * A launch configuration component to select an Isabelle session (logic) in the
 * given Isabelle directory.
 *
 * Depends on Isabelle directory selection component.
 *
 * @author Andrius Velykis
 */
class SessionSelectComponent(isaPathObservable: ObservableValue[Option[IsabellePaths]],
                             sessionDirsObservable: ObservableValue[Seq[String]])
    extends LaunchComponent[Option[String]] {

  def attributeName = IsabelleLaunchConstants.ATTR_SESSION
  
  private var sessionCheck = new SingleCheckStateProvider[CheckboxTreeViewer]
  private var progressMonitorPart: ProgressMonitorPart = _
  private var container: LaunchComponentContainer = _
  
  private var lastFinishedJob: Option[SessionLoadJob] = None
  private var sessionLoadJob: Option[SessionLoadJob] = None
  private var lastLoadError: Option[IStatus] = None
  
  
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
    isaPathObservable subscribe sessionLocsChanged
    // the same for session dirs change
    sessionDirsObservable subscribe sessionLocsChanged
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
    reloadAvailableSessions(Some(configuration))
    
    selectedSession = if (sessionName.isEmpty) None else Some(sessionName)
  }

  override def value = selectedSession

  private def selectedSession: Option[String] = {
    sessionCheck.checked map (_.toString)
  }
  
  private def selectedSession_= (value: Option[String]): Unit = {
    sessionCheck.checked = value
  }
  
  private def sessionLocsChanged() = reloadAvailableSessions()
  
  private def reloadAvailableSessions(configuration: Option[ILaunchConfiguration] = None) {
    
    val isaPath = isaPathObservable.value
    
    // if there is a config available, read more dirs from it, otherwise ask
    // the observable (the observable may be uninitialised)
    val configMoreDirs = configuration.map(conf =>
      configValue(conf, IsabelleLaunchConstants.ATTR_SESSION_DIRS, List[String]()))
    val moreDirs = configMoreDirs getOrElse sessionDirsObservable.value
    val resolvedDirs = moreDirs map resolvePath
    // allow only valid session dirs to avoid crashing the session lookup
    val moreDirsSafe = resolvedDirs filter IsabelleBuild.isSessionDir
    
    
    isaPath match {
      case None => {
        sessionLoadJob = None
        finishedLoadingSessions(None, Right(Nil), false)
      }
      
      case Some(path) => {
        
        val newLoadJob = Some(SessionLoadJob(path, moreDirsSafe))
        if (lastFinishedJob == newLoadJob) {
          // same job, avoid reloading
          sessionLoadJob = None
        } else {
          progressMonitorPart.beginTask("Loading available sessions...", IProgressMonitor.UNKNOWN)
          progressMonitorPart.getParent.setVisible(true)
          sessionLoadJob = newLoadJob
          sessionLoadJob.get.schedule()          
        }
      }
    }
  }

  private case class SessionLoadJob(isaPath: IsabellePaths,
                                    moreDirs: Seq[IPath])
    extends Job("Loading available sessions...") {
    
    // avoid parallel loads using the sync rule
    setRule(syncLoadRule)
    
    override protected def run(monitor: IProgressMonitor): IStatus = {

      val sessionLoad = IsabelleLaunch.availableSessions(isaPath, moreDirs)

      SWTUtil.asyncUnlessDisposed(Option(sessionCheck.viewer.getControl)) {
        finishedLoadingSessions(Some(this), sessionLoad, true)
      }

      // always return OK to avoid jarring error messages in UI - the error is reported
      // by logging here and in #finishedLoadingSessions() then #isValid()
//      sessionLoad fold ( err => err, success => Status.OK_STATUS )
      sessionLoad.left foreach IsabelleLaunchPlugin.log
      Status.OK_STATUS
    }
  }
  
  lazy val syncLoadRule = new ISchedulingRule {
    def contains(rule: ISchedulingRule) = rule == this
    def isConflicting(rule: ISchedulingRule) = rule == this
  }


  private def finishedLoadingSessions(loadJob: Option[SessionLoadJob],
                                      sessionsEither: Either[IStatus, List[String]],
                                      callback: Boolean) =
    if (sessionLoadJob == loadJob && !sessionCheck.viewer.getControl.isDisposed) {
      // correct loading job and config still open
      
      val sessions = sessionsEither.right getOrElse Nil
      
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
      lastLoadError = sessionsEither.left.toOption
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
      
    } else if (lastLoadError.isDefined) {
      Some(Left(lastLoadError.get.getMessage))

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
  
  
  // notify listeners
  private def configModified() = publish()


  /**
   * A FilteredTree with sessions checkbox tree viewer as main control
   */
  private class SessionFilteredTree(parent: Composite, treeStyle: Int)
      extends FilteredTree(parent, treeStyle, new PatternFilter(), true) {

    override protected def doCreateTreeViewer(parent: Composite, style: Int): TreeViewer =
      createCheckboxTreeViewer(parent, style)
  }
  
}
