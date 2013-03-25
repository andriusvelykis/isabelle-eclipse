package isabelle.eclipse.launch.tabs

import org.eclipse.debug.core.{ILaunchConfiguration, ILaunchConfigurationWorkingCopy}
import org.eclipse.jface.layout.{GridDataFactory, GridLayoutFactory}
import org.eclipse.swt.SWT
import org.eclipse.swt.events.{SelectionAdapter, SelectionEvent}
import org.eclipse.swt.widgets.{Button, Composite, Group}

import AccessibleUtil.addControlAccessibleListener
import isabelle.eclipse.launch.config.LaunchConfigUtil.configValue


/**
 * A launch component for Boolean flag, represented as radio buttons.
 */
class RadioFlagComponent(attributeName: String,
                         groupTitle: String,
                         trueLabel: String,
                         falseLabel: String,
                         defaultValue: => Boolean,
                         enableState: Option[ObservableValue[Boolean]] = None)
    extends LaunchComponent[Boolean] {

  var group: Group = _
  var trueButton: Button = _
  var falseButton: Button = _
  
  /**
   * Creates the controls needed to edit the location attribute of an external tool
   */
  override def createControl(parent: Composite, container: LaunchComponentContainer) {

    group = new Group(parent, SWT.NONE)
    group.setText(groupTitle)
    group.setLayout(GridLayoutFactory.swtDefaults.numColumns(2).create)
    group.setLayoutData(GridDataFactory.fillDefaults.grab(true, false).create)
    group.setFont(parent.getFont)
    
    val adapter = new SelectionAdapter {
      override def widgetSelected(e: SelectionEvent) = configModified()
    }
    
    def radioButton(label: String): Button = {
      val btn = container.createRadioButton(group, label)
      btn.setLayoutData(GridDataFactory.fillDefaults.grab(true, false).span(2, 1).create)
      btn.addSelectionListener(adapter)
      
      addControlAccessibleListener(btn, group.getText + " " + label)
      
      btn
    }
    
    trueButton = radioButton(trueLabel)
    falseButton = radioButton(falseLabel)
    
    // on enable state change (if available), update the controls
    enableState foreach (_ subscribe updateEnableState)
  }


  override def initializeFrom(configuration: ILaunchConfiguration) {
    val value = configValue(configuration, attributeName, defaultValue)
    
    selectedValue = value
    
    if (enableState.isDefined) {
      updateEnableState()
    }
  }

  override def value = selectedValue

  def selectedValue: Boolean = trueButton.getSelection

  private def selectedValue_=(value: Boolean): Unit = {
    trueButton.setSelection(value)
    falseButton.setSelection(!value)
  }

  override def performApply(configuration: ILaunchConfigurationWorkingCopy) {
    configuration.setAttribute(attributeName, selectedValue)
  }

  // always valid
  override def isValid(configuration: ILaunchConfiguration,
                       newConfig: Boolean): Option[Either[String, String]] = None


  // notify listeners
  private def configModified() = publish()

  
  private def updateEnableState() {
    enableState foreach { state =>
      val enabled = state.value
      
      trueButton.setEnabled(enabled)
      falseButton.setEnabled(enabled)
      group.setEnabled(enabled)
    }
  }
  
}
