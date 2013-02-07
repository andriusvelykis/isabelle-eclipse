package isabelle.eclipse.launch.tabs

import org.eclipse.debug.core.{ILaunchConfiguration, ILaunchConfigurationWorkingCopy}
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab
import org.eclipse.jface.dialogs.Dialog
import org.eclipse.jface.layout.GridLayoutFactory
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.{Button, Composite}

import ObservableUtil.NotifyPublisher
import isabelle.eclipse.launch.config.LaunchConfigUtil.configValue


/**
 * Launch configuration tab that consists of several components describing properties
 * and UI to select them. Allows defining components independently and then join here in UI.
 * 
 * @author Andrius Velykis
 */
abstract class LaunchComponentTab(components: List[LaunchComponent[_]])
    extends AbstractLaunchConfigurationTab with LaunchComponentContainer {

  lazy val FIRST_EDIT = "editedByTab-" + (Option(getId) getOrElse getName)
  
  private var initializing = false
  private var userEdited = false
  
  override def createControl(parent:Composite) {
    
    val mainComposite = new Composite(parent, SWT.NONE)
    setControl(mainComposite)
    
    mainComposite.setFont(parent.getFont)
    mainComposite.setLayout(GridLayoutFactory.swtDefaults.create)
    
    components foreach (_.createControl(mainComposite, this))
    
    createVerticalSpacer(mainComposite, 1)
    
    Dialog.applyDialogFont(parent)

    // add listener to each component change
    components foreach (_.subscribeFun(_ => configModified()))
  }
  
  def createPushButton(parent: Composite, label: String): Button =
    createPushButton(parent, label, null)

  
  private def configModified() {
    if (!initializing) {
      setDirty(true)
      userEdited = true
      updateLaunchConfigurationDialog()
    }
  }


  override def setDefaults(configuration: ILaunchConfigurationWorkingCopy) {
    configuration.setAttribute(FIRST_EDIT, true)
  }

  override def initializeFrom(configuration: ILaunchConfiguration) {
    initializing = true
    components foreach (_.initializeFrom(configuration))
    initializing = false
    
    setDirty(false)
  }
  
  override def performApply(configuration: ILaunchConfigurationWorkingCopy) {
    
    components foreach (_.performApply(configuration))
    
    if (userEdited) {
      configuration.removeAttribute(FIRST_EDIT)
    }
  }
  
    
  override def isValid(configuration: ILaunchConfiguration): Boolean = {
    setErrorMessage(null)
    setMessage(null)
    
    val newConfig = configValue(configuration, FIRST_EDIT, false)
    
    val errStream = components.toStream map (_.isValid(configuration, newConfig)) flatten
    
    errStream.headOption match {
      case Some(error) => {
        // display errors
        error.fold( setErrorMessage, setMessage )
        // report invalid
        false
      }
      case None => true
    }
  }

  override def activated(workingCopy: ILaunchConfigurationWorkingCopy) {}
  
  override def deactivated(workingCopy: ILaunchConfigurationWorkingCopy) {}
  
}
