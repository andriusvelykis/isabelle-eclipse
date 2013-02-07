package isabelle.eclipse.launch.tabs

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
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.{Composite, Group}
import org.eclipse.ui.dialogs.{FilteredTree, PatternFilter}

import AccessibleUtil.addControlAccessibleListener
import ObservableUtil.NotifyPublisher
import isabelle.eclipse.launch.config.{IsabelleLaunch, IsabelleLaunchConstants}
import isabelle.eclipse.launch.config.LaunchConfigUtil.{configValue, setConfigValue}


/**
 * A launch configuration component to select an Isabelle session (logic) in the
 * given Isabelle directory.
 * 
 * Depends on Isabelle directory selection component.
 * 
 * @author Andrius Velykis
 */
class SessionSelectComponent(isaPathObservable: ObservableValue[Option[String]])
    extends LaunchComponent[Option[String]] {

  def attributeName = IsabelleLaunchConstants.ATTR_SESSION
  
  private var sessionCheck = new SingleCheckStateProvider[CheckboxTreeViewer]
  
  
  /**
   * Creates the controls needed to select logic for the Isabelle installation.
   */
  override def createControl(parent: Composite, container: LaunchComponentContainer) {
    
    val group = new Group(parent, SWT.NONE)
    group.setText("&Session:")
    
    group.setLayout(GridLayoutFactory.swtDefaults.create)
    group.setLayoutData(GridDataFactory.fillDefaults.grab(true, true).create)
    group.setFont(parent.getFont)
    
    val filteredSessionsViewer = new SessionFilteredTree(group, SWT.BORDER)
    val sessionsViewer = filteredSessionsViewer.getViewer
    addControlAccessibleListener(sessionsViewer.getControl, group.getText)

    
    // on config change in Isabelle path, update the session selection
    // (only do after UI initialisation)
    isaPathObservable.subscribeFun(_ => isaPathChanged())
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
    val selectedSessions = sessionCheck.viewer.getCheckedElements
    selectedSessions.headOption map (_.toString)
  }
  
  private def selectedSession_= (value: Option[String]): Unit = {
    sessionCheck.checked = value
  }
  
  private def isaPathChanged() = reloadAvailableSessions()
  
  private def reloadAvailableSessions() {
    
    val isaPath = isaPathObservable.value

    val sessionsOpt = isaPath flatMap (path =>
      IsabelleLaunch.availableSessions(path).right.toOption)
    
    val sessions = sessionsOpt getOrElse Nil
    
    sessionCheck.viewer.setInput(sessions.toArray)
    
    // TODO suggest some default value, e.g. HOL?
    if (sessions.size == 1) {
      selectedSession = sessions.headOption
    }
  }
  
  
  override def performApply(configuration: ILaunchConfigurationWorkingCopy) {
    setConfigValue(configuration, attributeName, selectedSession)
  }

  
  override def isValid(configuration: ILaunchConfiguration,
                       newConfig: Boolean): Option[Either[String, String]] =
    if (sessionCheck.viewer.getTree.getItemCount == 0) {
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
