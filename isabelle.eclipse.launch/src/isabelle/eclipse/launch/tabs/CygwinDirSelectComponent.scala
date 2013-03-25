package isabelle.eclipse.launch.tabs

import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.swt.events.{ModifyEvent, ModifyListener}
import org.eclipse.swt.widgets.Composite

import isabelle.eclipse.launch.config.{IsabelleLaunchConstants, WinDirLaunch}


/**
 * A specialization of directory selection component for Cygwin root selection.
 * 
 * Suggests Cygwin path within Isabelle if not modified manually.
 *
 * @author Andrius Velykis
 */
class CygwinDirSelectComponent(isaPathObservable: ObservableValue[Option[String]])
    extends DirSelectComponent {

  private var locationSetManually = false

  override def attributeName = IsabelleLaunchConstants.ATTR_CYGWIN_LOCATION
  
  override protected def locationLabel =
    "Cygwin directory location (e.g. within Isabelle installation):"

  override protected def defaultLocationMessage =
    "Please specify the location of Cygwin directory (e.g. within Isabelle installation)"

  override protected def notDirectoryMessage =
    "Cygwin location specified is not a directory"

  override protected def targetTitle = "Cygwin"

  override def createControl(parent: Composite, container: LaunchComponentContainer) {
    super.createControl(parent, container)

    locationField.addModifyListener(new ModifyListener {
      def modifyText(e: ModifyEvent) = locationModified()
    })

    // on config change in Isabelle path, update the Cygwin suggestion selection
    // (only do after UI initialisation)
    isaPathObservable subscribe isaPathChanged
  }

  override def initializeFrom(configuration: ILaunchConfiguration) {
    super.initializeFrom(configuration)

    initializing = false
    updateCygwinSuggestion()
    initializing = true
  }

  private def locationModified() =
    if (!initializing) locationSetManually = true


  private def isaPathChanged() = updateCygwinSuggestion()
  
  private def updateCygwinSuggestion() = if (!locationSetManually) {
    // only suggest if not modified manually

    val isaPath = isaPathObservable.value

    val cygwinPath = isaPath flatMap WinDirLaunch.isabelleCygwinPath

    cygwinPath foreach { path =>
      // do not fire notifications when suggesting (assume isabelle path notifications are enough)
      initializing = false
      locationField.setText(path)
      initializing = true
    }
  }

}
