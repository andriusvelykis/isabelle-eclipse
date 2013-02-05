package isabelle.eclipse.launch.tabs

import java.io.File

import org.eclipse.core.databinding.observable.value.{IValueChangeListener, ValueChangeEvent}
import org.eclipse.debug.core.{ILaunchConfiguration, ILaunchConfigurationWorkingCopy}
import org.eclipse.jface.databinding.swt.SWTObservables
import org.eclipse.jface.dialogs.IDialogConstants
import org.eclipse.jface.layout.{GridDataFactory, GridLayoutFactory}
import org.eclipse.swt.SWT
import org.eclipse.swt.events.{SelectionAdapter, SelectionEvent}
import org.eclipse.swt.widgets.{Button, Composite, DirectoryDialog, Group, Text}

import AccessibleUtil.addControlAccessibleListener
import isabelle.eclipse.launch.IsabelleLaunchConstants
import isabelle.eclipse.launch.config.IsabelleLaunch.{configValue, setConfigValue}


/**
 * A launch component to select and configure Isabelle installation directory.
 * 
 * @author Andrius Velykis
 */
class DirSelectComponent extends LaunchComponent[Option[String]] {

  def attributeName = IsabelleLaunchConstants.ATTR_LOCATION
  
  private var locationField: Text = _

  protected def locationLabel = "Isabelle location:"

  protected def defaultLocationMessage =
    "Please specify the root directory of the Isabelle installation you would like to configure"

  protected def notDirectoryMessage =
    "Isabelle installation location specified is not a directory"

  /**
   * Creates the controls needed to edit the location attribute of an external tool
   */
  override def createControl(parent: Composite) {

    val group = new Group(parent, SWT.NONE)
    group.setText(locationLabel)

    val gridDataFillHorizontal =
      GridDataFactory.fillDefaults.align(SWT.FILL, SWT.BEGINNING).grab(true, false)

    group.setLayout(GridLayoutFactory.swtDefaults.create)
    group.setLayoutData(gridDataFillHorizontal.create)
    group.setFont(parent.getFont)

    locationField = new Text(group, SWT.BORDER)
    locationField.setLayoutData(
      gridDataFillHorizontal.hint(IDialogConstants.ENTRY_FIELD_WIDTH, SWT.DEFAULT).create)
    addControlAccessibleListener(locationField, group.getText)

    val buttonComposite = new Composite(group, SWT.NONE)
    buttonComposite.setLayout(GridLayoutFactory.fillDefaults.create)
    buttonComposite.setLayoutData(GridDataFactory.swtDefaults.align(SWT.END, SWT.CENTER).create)
    buttonComposite.setFont(parent.getFont)

    val fileLocationButton = new Button(buttonComposite, SWT.PUSH)
    fileLocationButton.setFont(parent.getFont)
    fileLocationButton.setText("Browse File System...")
    fileLocationButton.setLayoutData(GridDataFactory.swtDefaults.align(SWT.END, SWT.CENTER).create)
    addControlAccessibleListener(fileLocationButton, group.getText + " " + fileLocationButton.getText)

    fileLocationButton.addSelectionListener(new SelectionAdapter {
      override def widgetSelected(e: SelectionEvent) = browseSelected()
    })

    // listen to location changes with delay
    val delayedLocationValue =
      SWTObservables.observeDelayedValue(1000,
        SWTObservables.observeText(locationField, SWT.Modify))

    delayedLocationValue.addValueChangeListener(new IValueChangeListener {
      override def handleValueChange(event: ValueChangeEvent) = configModified()
    })
  }

  override def initializeFrom(configuration: ILaunchConfiguration) {
    val dir = configValue(configuration, attributeName, "")

    selectedDir = if (dir.isEmpty) None else Some(dir)
  }

  private def locationFieldChecked: Option[Text] = Option(locationField) filterNot (_.isDisposed)

  def selectedDir: Option[String] =
    locationFieldChecked map (_.getText.trim) filterNot (_.isEmpty())

  private def selectedDir_=(value: Option[String]): Unit = {
    val dirText = value getOrElse ""
    locationField.setText(dirText)
  }

  override def performApply(configuration: ILaunchConfigurationWorkingCopy) {
    setConfigValue(configuration, attributeName, selectedDir)
  }

  override def isValid(configuration: ILaunchConfiguration,
                       newConfig: Boolean): Option[Either[String, String]] = {

    val dirError = selectedDir match {

      // either urge to select for new config, or report error
      case None => Some("Isabelle installation location cannot be empty")

      case Some(location) => {
        // something is entered - validate if correct directory

        val file = new File(location)
        if (!file.exists) {
          Some("Isabelle installation location does not exist")
        } else if (!file.isDirectory) {
          Some(notDirectoryMessage)
        } else {
          // valid location - no errors
          None
        }
      }

    }

    // if new config, use default message, otherwise the error
    dirError map { err => if (newConfig) Right(defaultLocationMessage) else Left(err) }
  }

  private def configModified() {
    // notify listeners
    publish(selectedDir)
  }

  private def browseSelected() {
    val dirOpt = browseDir()
    dirOpt foreach (dir => selectedDir = Some(dir))
  }
  
  protected def shell = locationField.getShell

  protected def browseDir(): Option[String] = {

    val dialog = new DirectoryDialog(shell, SWT.SAVE)
    dialog.setMessage("Select a Isabelle installation directory:")
    selectedDir foreach dialog.setFilterPath

    Option(dialog.open)
  }
}
