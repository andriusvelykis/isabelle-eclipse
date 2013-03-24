package isabelle.eclipse.launch.tabs

import java.io.File

import org.eclipse.debug.core.{ILaunchConfiguration, ILaunchConfigurationWorkingCopy}
import org.eclipse.jface.dialogs.IDialogConstants
import org.eclipse.jface.layout.{GridDataFactory, GridLayoutFactory}
import org.eclipse.swt.SWT
import org.eclipse.swt.events.{ModifyEvent, ModifyListener, SelectionAdapter, SelectionEvent}
import org.eclipse.swt.widgets.{Composite, DirectoryDialog, Group, Text}

import isabelle.eclipse.launch.config.IsabelleLaunchConstants
import isabelle.eclipse.launch.config.LaunchConfigUtil.{configValue, setConfigValue}

import AccessibleUtil.addControlAccessibleListener


/**
 * A launch component to select and configure Isabelle installation directory.
 * 
 * @author Andrius Velykis
 */
class DirSelectComponent extends LaunchComponent[Option[String]] {

  def attributeName = IsabelleLaunchConstants.ATTR_LOCATION
  
  protected var locationField: Text = _
  
  protected var initializing = false
  /** Helps listening to typing changes with delay */
  private var typingDelayHelper = new TypingDelayHelper(1000)

  protected def locationLabel = "Isabelle location:"

  protected def defaultLocationMessage =
    "Please specify the root directory of the Isabelle installation you would like to configure"

  protected def notDirectoryMessage =
    "Isabelle installation location specified is not a directory"

  protected def targetTitle = "Isabelle installation"

  /**
   * Creates the controls needed to edit the location attribute of an external tool
   */
  override def createControl(parent: Composite, container: LaunchComponentContainer) {

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

    val fileLocationButton = container.createPushButton(buttonComposite, "Browse File System...")
    fileLocationButton.setLayoutData(GridDataFactory.swtDefaults.align(SWT.END, SWT.CENTER).create)
    addControlAccessibleListener(fileLocationButton, group.getText + " " + fileLocationButton.getText)

    fileLocationButton.addSelectionListener(new SelectionAdapter {
      override def widgetSelected(e: SelectionEvent) = browseSelected()
    })

    locationField.addModifyListener(new ModifyListener {
      def modifyText(e: ModifyEvent) = locationModified()
    })
  }
  
  private def locationModified() {
    if (!initializing) {
      // schedule delayed event
      typingDelayHelper.scheduleCallback(Some(locationField.getDisplay)) {
        configModified()
      }
    }
  }
  
  override def dispose() {
    typingDelayHelper.stop()
    super.dispose()
  }

  override def initializeFrom(configuration: ILaunchConfiguration) {
    initializing = true
    val dir = configValue(configuration, attributeName, "")

    selectedDir = if (dir.isEmpty) None else Some(dir)
    initializing = false
  }

  private def locationFieldChecked: Option[Text] = Option(locationField) filterNot (_.isDisposed)

  override def value = selectedDir

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
      case None => Some(targetTitle + " location cannot be empty")

      case Some(location) => {
        // something is entered - validate if correct directory

        val file = new File(location)
        if (!file.exists) {
          Some(targetTitle + " location does not exist")
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

  // notify listeners
  private def configModified() = publish()


  private def browseSelected() {
    val dirOpt = browseDir()
    dirOpt foreach (dir => selectedDir = Some(dir))
  }
  
  protected def shell = locationField.getShell

  protected def browseDir(): Option[String] = {

    val dialog = new DirectoryDialog(shell, SWT.NONE)
    dialog.setMessage("Select a " + targetTitle + " directory:")
    selectedDir foreach dialog.setFilterPath

    Option(dialog.open)
  }
}
