package isabelle.eclipse.launch.tabs

import org.eclipse.debug.core.{ILaunchConfiguration, ILaunchConfigurationWorkingCopy}
import org.eclipse.jface.layout.{GridDataFactory, GridLayoutFactory}
import org.eclipse.swt.SWT
import org.eclipse.swt.events.{SelectionAdapter, SelectionEvent}
import org.eclipse.swt.widgets.{Button, Composite}

import AccessibleUtil.addControlAccessibleListener
import isabelle.eclipse.launch.config.LaunchConfigUtil.configValue


/**
 * A launch component for Boolean flag, represented as checkbox button.
 */
class CheckComponent(attributeName: String,
                     label: String,
                     defaultValue: => Boolean)
    extends LaunchComponent[Boolean] with ObservableValue[Boolean] {

  var button: Button = _
  
  /**
   * Creates the controls needed to edit the location attribute of an external tool
   */
  override def createControl(parent: Composite, container: LaunchComponentContainer) {

    val group = new Composite(parent, SWT.NONE)
    group.setLayout(GridLayoutFactory.fillDefaults.numColumns(2).create)
    group.setLayoutData(GridDataFactory.fillDefaults.grab(true, false).create)
    group.setFont(parent.getFont)
    
    val adapter = new SelectionAdapter {
      override def widgetSelected(e: SelectionEvent) = configModified()
    }
    
    button = container.createCheckButton(group, label)
    button.setLayoutData(GridDataFactory.fillDefaults.grab(true, false).span(2, 1).create)
    button.addSelectionListener(adapter)
    
    addControlAccessibleListener(button,label)
  }


  override def initializeFrom(configuration: ILaunchConfiguration) {
    val value = configValue(configuration, attributeName, defaultValue)
    
    selectedValue = value
  }

  def selectedValue: Boolean = button.getSelection

  private def selectedValue_=(value: Boolean): Unit = button.setSelection(value)
  
  override def value = selectedValue

  override def performApply(configuration: ILaunchConfigurationWorkingCopy) {
    configuration.setAttribute(attributeName, selectedValue)
  }

  // always valid
  override def isValid(configuration: ILaunchConfiguration,
                       newConfig: Boolean): Option[Either[String, String]] = None


  // notify listeners
  private def configModified() = publish()
  
}
