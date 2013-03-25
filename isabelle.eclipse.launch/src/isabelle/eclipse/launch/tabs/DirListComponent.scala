package isabelle.eclipse.launch.tabs

import scala.collection.JavaConverters._

import org.eclipse.core.resources.{IWorkspaceRoot, ResourcesPlugin}
import org.eclipse.core.runtime.{IPath, Path}
import org.eclipse.core.variables.VariablesPlugin
import org.eclipse.debug.core.{ILaunchConfiguration, ILaunchConfigurationWorkingCopy}
import org.eclipse.jface.dialogs.IDialogConstants
import org.eclipse.jface.layout.{GridDataFactory, GridLayoutFactory}
import org.eclipse.jface.viewers.{
  ArrayContentProvider,
  ISelectionChangedListener,
  IStructuredSelection,
  LabelProvider,
  SelectionChangedEvent,
  TableViewer
}
import org.eclipse.swt.SWT
import org.eclipse.swt.events.{KeyAdapter, KeyEvent, SelectionAdapter, SelectionEvent}
import org.eclipse.swt.widgets.{Button, Composite, DirectoryDialog, Label}
import org.eclipse.ui.{ISharedImages, PlatformUI}
import org.eclipse.ui.dialogs.ContainerSelectionDialog

import AccessibleUtil.addControlAccessibleListener
import isabelle.eclipse.core.app.IsabelleBuild
import isabelle.eclipse.launch.IsabelleLaunchPlugin
import isabelle.eclipse.launch.config.IsabelleLaunchConstants
import isabelle.eclipse.launch.config.LaunchConfigUtil.{configValue, resolvePath}


/**
 * A launch component to select additional directories for Isabelle sessions.
 * 
 * @author Andrius Velykis
 */
class DirListComponent extends LaunchComponent[Seq[String]] {

  def attributeName = IsabelleLaunchConstants.ATTR_SESSION_DIRS
  
  private var dirTableViewer: TableViewer = _
  private var _selectedDirs: Seq[String] = Seq()
  
  private lazy val dialogSettings = IsabelleLaunchPlugin.plugin.getDialogSettings

  /**
   * Creates the controls needed to edit the location attribute of an external tool
   */
  override def createControl(parent: Composite, container: LaunchComponentContainer) {

    val group = new Composite(parent, SWT.NONE)

    val gridDataFill = GridDataFactory.fillDefaults.grab(true, true)

    group.setLayout(GridLayoutFactory.fillDefaults.numColumns(2).create)
    group.setLayoutData(gridDataFill.hint(IDialogConstants.ENTRY_FIELD_WIDTH, 50).create)
    group.setFont(parent.getFont)
    
    val label = new Label(group, SWT.NONE)
    label.setText("Additional Isabelle session directories:")
    label.setFont(parent.getFont)
    label.setLayoutData(GridDataFactory.swtDefaults.span(2, 1).create)

    dirTableViewer = new TableViewer(group, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER)

    dirTableViewer.getControl.setLayoutData(
        gridDataFill.hint(IDialogConstants.ENTRY_FIELD_WIDTH, 50).create)

    // use folder images for the image provider
    dirTableViewer.setLabelProvider(new LabelProvider {
      override def getImage(elem: Any) =
        PlatformUI.getWorkbench.getSharedImages.getImage(ISharedImages.IMG_OBJ_FOLDER)
    })
    dirTableViewer.setContentProvider(new ArrayContentProvider)
    
    dirTableViewer.setInput(Array())
    
    addControlAccessibleListener(dirTableViewer.getControl, label.getText)

    val buttonComposite = new Composite(group, SWT.NONE)
    buttonComposite.setLayout(GridLayoutFactory.fillDefaults.create)
    buttonComposite.setLayoutData(GridDataFactory.fillDefaults.grab(false, true).create)
    buttonComposite.setFont(parent.getFont)
    
    def pushButton(buttonLabel: String, onClick: () => Unit): Button =
      createPushButton(buttonComposite, container, label.getText, buttonLabel, onClick)
    
    val addDirButton = pushButton("Add directories...", addWorkspaceDir)
    val addExternalDirButton = pushButton("Add external...", addExternalDir)
    val removeDirButton = pushButton("Remove", removeSelectedDirs)
    removeDirButton.setEnabled(false)
    
    
    // enable remove button if selected
    dirTableViewer.addSelectionChangedListener(new ISelectionChangedListener {
      override def selectionChanged(event: SelectionChangedEvent) =
        removeDirButton.setEnabled(!event.getSelection.isEmpty)
    })

    // allow deleting with keyboard
    dirTableViewer.getTable.addKeyListener(new KeyAdapter {
      override def keyPressed(event: KeyEvent) {
        if (!dirTableViewer.getSelection.isEmpty &&
          event.character == SWT.DEL && event.stateMask == 0) {
          removeSelectedDirs()
        }
      }
    })
  }

  private def createPushButton(parent: Composite,
                               container: LaunchComponentContainer,
                               groupText: String,
                               label: String,
                               onClick: () => Unit): Button = {
    val button = container.createPushButton(parent, label)
    button.addSelectionListener(new SelectionAdapter {
      override def widgetSelected(e: SelectionEvent) = onClick()
    })

    button.setLayoutData(
      GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, false).create)

    addControlAccessibleListener(button, groupText + " " + button.getText)

    button
  }


  override def initializeFrom(configuration: ILaunchConfiguration) {
    val dirs = configValue(configuration, attributeName, List[String]())

    selectedDirs = dirs
  }

  override def value = selectedDirs

  def selectedDirs: Seq[String] = _selectedDirs

  private def selectedDirs_=(value: Seq[String]): Unit = {
    _selectedDirs = value
    dirTableViewer.setInput(_selectedDirs.toArray)
  }

  override def performApply(configuration: ILaunchConfigurationWorkingCopy) {
    configuration.setAttribute(attributeName, selectedDirs.asJava)
  }

  override def isValid(configuration: ILaunchConfiguration,
                       newConfig: Boolean): Option[Either[String, String]] = {
    
    // check that selected dirs are indeed directories
    val selectedPaths = selectedDirs.toStream map (dir => (dir, resolvePath(dir)))
    
    val invalidPath = selectedPaths find { case (_, path) => !IsabelleBuild.isSessionDir(path) }
    
    invalidPath map { case (dir, _) => 
      Left("Invalid Isabelle session directory (no session root): " + dir) }
  }


  // notify listeners
  private def configModified() = publish()

  
  /**
   * Allows the user to enter workspace directories
   */
  private def addWorkspaceDir() {

    val workspaceRoot = ResourcesPlugin.getWorkspace.getRoot
    
    val dialog = new ContainerSelectionDialog(
      dirTableViewer.getControl.getShell,
      workspaceRoot,
      false,
      "Select workspace Isabelle session directory:")

    dialog.open
    
    val result = Option(dialog.getResult)
    
    if (result.isDefined) {
      selectedDirs = selectedDirs ++ result.get.toSeq.map(p => containerPathStr(workspaceRoot, p))
      configModified()
    }
  }

  private def containerPathStr(root: IWorkspaceRoot, path: Any): String = path match {
    case p: IPath => {

      // generate a variable expression with workspace location
      val varExpression = VariablesPlugin.getDefault.getStringVariableManager.
        generateVariableExpression("workspace_loc", p.toString)

      varExpression
    }
    case _ => String.valueOf(path)
  }
  
  /**
   * Allows the user to enter external directories
   */
  private def addExternalDir() {
    val lastUsedPath = Option(dialogSettings.get(IsabelleLaunchConstants.DIALOG_LAST_EXT_DIR))
    
    val dialog = new DirectoryDialog(dirTableViewer.getControl.getShell, SWT.SHEET)
    dialog.setMessage("Select a Isabelle session directory:")
    lastUsedPath foreach dialog.setFilterPath

    val result = Option(dialog.open)
    
    if (result.isDefined) {
      val selectedPath: IPath = new Path(result.get)
      
      selectedDirs = selectedDirs :+ selectedPath.toOSString
      dialogSettings.put(IsabelleLaunchConstants.DIALOG_LAST_EXT_DIR, selectedPath.toOSString)
      configModified()
    }
  }
  
  private def removeSelectedDirs() {
    
    val sel = dirTableViewer.getSelection.asInstanceOf[IStructuredSelection]
    val selectedItems = sel.toList().asScala
    
    // remove selection
    selectedDirs = selectedDirs diff selectedItems
    configModified()
  }
}
