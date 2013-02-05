package isabelle.eclipse.launch.tabs

import org.eclipse.debug.core.{ILaunchConfiguration, ILaunchConfigurationWorkingCopy}
import org.eclipse.jface.dialogs.IDialogConstants
import org.eclipse.jface.layout.{GridDataFactory, GridLayoutFactory}
import org.eclipse.jface.resource.{JFaceResources, LocalResourceManager}
import org.eclipse.jface.viewers.{
  ArrayContentProvider,
  CheckStateChangedEvent,
  CheckboxTableViewer,
  ICheckStateListener
}
import org.eclipse.swt.SWT
import org.eclipse.swt.events.{SelectionAdapter, SelectionEvent}
import org.eclipse.swt.widgets.{Button, Composite, Group}

import AccessibleUtil.addControlAccessibleListener
import isabelle.eclipse.launch.config.{IsabelleLaunchConstants, IsabelleLaunch}
import isabelle.eclipse.launch.config.LaunchConfigUtil.{configValue, setConfigValue}

/**
 * A launch configuration component to select an Isabelle session (logic) in the
 * given Isabelle directory.
 * 
 * Depends on Isabelle directory selection component.
 * 
 * @author Andrius Velykis
 */
class SessionSelectComponent(isaPathComponent: LaunchComponent[Option[String]],
                             isaPathValue: () => Option[String])
    extends LaunchComponent[Option[String]] {

  def attributeName = IsabelleLaunchConstants.ATTR_SESSION
  
  private var sessionsViewer: CheckboxTableViewer = _
  
  
  /**
   * Creates the controls needed to select logic for the Isabelle installation.
   */
  override def createControl(parent: Composite) {
    
    val group = new Group(parent, SWT.NONE)
    group.setText("&Session:")
    
    val gridDataFill = GridDataFactory.fillDefaults.grab(true, true)
    
    group.setLayout(GridLayoutFactory.swtDefaults.create)
    group.setLayoutData(gridDataFill.create)
    group.setFont(parent.getFont)
    
    sessionsViewer = CheckboxTableViewer.newCheckList(group, 
        SWT.CHECK | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION)
    
    sessionsViewer.getControl.setLayoutData(
        gridDataFill.hint(IDialogConstants.ENTRY_FIELD_WIDTH, 50).create)

    val resourceManager = new LocalResourceManager(
      JFaceResources.getResources, sessionsViewer.getControl)
    
    sessionsViewer.setLabelProvider(new SessionLabelProvider(resourceManager))
    sessionsViewer.setContentProvider(new ArrayContentProvider)
    sessionsViewer.addCheckStateListener(new SingleCheckedListener(sessionsViewer))
    sessionsViewer.setInput(Array())
    
    sessionsViewer.addCheckStateListener(new ICheckStateListener {
      override def checkStateChanged(event: CheckStateChangedEvent) = configModified()
    })
    
    addControlAccessibleListener(sessionsViewer.getControl, group.getText)
    
    val infoComposite = new Composite(group, SWT.NONE)
    infoComposite.setLayout(GridLayoutFactory.fillDefaults.create)
    infoComposite.setLayoutData(
        GridDataFactory.swtDefaults.align(SWT.END, SWT.BEGINNING).grab(true, false).create)
    
    infoComposite.setFont(parent.getFont)
    
    val sessionsLogButton = new Button(infoComposite, SWT.PUSH)
    sessionsLogButton.setFont(parent.getFont)
    sessionsLogButton.setText("Show Log...")
    sessionsLogButton.setLayoutData(GridDataFactory.swtDefaults.align(SWT.END, SWT.CENTER).create)
    addControlAccessibleListener(sessionsLogButton, sessionsLogButton.getText)
    
    sessionsLogButton.addSelectionListener(new SelectionAdapter {
      override def widgetSelected(e: SelectionEvent) = sessionsLogSelected()
    })
    
    
    // on config change in Isabelle path, update the session selection
    // (only do after UI initialisation)
    isaPathComponent.onConfigChanged(_ => isaPathChanged())
  }
  
  
  override def initializeFrom(configuration: ILaunchConfiguration) {
    val sessionName = configValue(configuration, attributeName, "")
    reloadAvailableSessions()
    
    selectedSession = if (sessionName.isEmpty) None else Some(sessionName)
  }


  private def selectedSession: Option[String] = {
    val selectedSessions = sessionsViewer.getCheckedElements
    selectedSessions.headOption map (_.toString)
  }
  
  private def selectedSession_= (value: Option[String]): Unit = {
    val selection: Array[Object] = value.toArray
    sessionsViewer.setCheckedElements(selection)
  }
  
  private def isaPathChanged() = reloadAvailableSessions()
  
  private def reloadAvailableSessions() {
    
    val isaPath = isaPathValue()

    val sessionsOpt = isaPath flatMap (path =>
      IsabelleLaunch.availableSessions(path).right.toOption)
    
    val sessions = sessionsOpt getOrElse Nil
    
    sessionsViewer.setInput(sessions.toArray)
    
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
    if (sessionsViewer.getTable.getItemCount == 0) {
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

  private def sessionsLogSelected() {
    val logDialog =
      new LogDialog(sessionsViewer.getControl.getShell, "Logics Query Log",
        // FIXME
        "Querying Isabelle logics available in the indicated location.", "TODO", SWT.NONE)

    logDialog.open()
  }
  
}
