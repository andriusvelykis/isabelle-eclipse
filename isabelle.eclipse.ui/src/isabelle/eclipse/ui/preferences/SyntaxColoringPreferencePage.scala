package isabelle.eclipse.ui.preferences

import scala.language.implicitConversions
import scala.PartialFunction.condOpt

import org.eclipse.core.runtime.preferences.InstanceScope
import org.eclipse.jface.layout.{GridDataFactory, GridLayoutFactory, PixelConverter}
import org.eclipse.jface.preference.{ColorSelector, PreferenceConverter, PreferencePage}
import org.eclipse.jface.text.source.SourceViewer
import org.eclipse.jface.viewers.{
  DoubleClickEvent,
  IDoubleClickListener,
  ISelection,
  ISelectionChangedListener,
  IStructuredSelection,
  ITreeContentProvider,
  LabelProvider,
  SelectionChangedEvent,
  StructuredSelection,
  TreeViewer,
  Viewer
}
import org.eclipse.swt.SWT
import org.eclipse.swt.events.{SelectionAdapter, SelectionEvent}
import org.eclipse.swt.widgets.{Button, Composite, Control, Label, Link, Scrollable}
import org.eclipse.ui.{IWorkbench, IWorkbenchPreferencePage}
import org.eclipse.ui.dialogs.PreferencesUtil
import org.eclipse.ui.preferences.ScopedPreferenceStore

import isabelle.eclipse.ui.internal.IsabelleUIPlugin

import IsabelleSyntaxClasses._
import OverlayPreferenceStore._


/**
 * The preference page to adjust syntax colouring of Isabelle syntax classes.
 * 
 * Page adapted from Scala-IDE: 
 * scala.tools.eclipse.properties.syntaxcolouring.SyntaxColouringPreferencePage
 * 
 * @see org.eclipse.jdt.internal.ui.preferences.JavaEditorColoringConfigurationBlock
 */
class SyntaxColoringPreferencePage extends PreferencePage with IWorkbenchPreferencePage {

  /*
   * Make sure to create a new Scoped preference store - do not reuse the one from plugin.
   * This is because changes in ScopedPreferenceStore do not get notified. Instead we need
   * a different ScopedPreferenceStore to push changes to IEclipsePreferences node, which
   * is then picked up by Plugin preference store.
   */
  val writePrefStore = new ScopedPreferenceStore(
      InstanceScope.INSTANCE, IsabelleUIPlugin.plugin.pluginId)

  setPreferenceStore(writePrefStore)
  private val overlayStore = makeOverlayPreferenceStore

  private var syntaxForegroundColorEditor: ColorSelector = _
  private var syntaxBackgroundColorEditor: ColorSelector = _
  private var foregroundColorEnabledCheckBox: Button = _
  private var backgroundColorEnabledCheckBox: Button = _
  private var foregroundColorButton: Button = _
  private var backgroundColorButton: Button = _
  private var boldCheckBox: Button = _
  private var italicCheckBox: Button = _
  private var underlineCheckBox: Button = _
  private var treeViewer: TreeViewer = _
  private var previewer: SourceViewer = _

  def init(workbench: IWorkbench) {}

  def createContents(parent: Composite): Control = {
    initializeDialogUnits(parent)

    val scrolled = new ScrolledPageContent(parent, SWT.H_SCROLL | SWT.V_SCROLL)
    scrolled.setExpandHorizontal(true)
    scrolled.setExpandVertical(true)

    val control = createSyntaxPage(scrolled)

    scrolled.setContent(control)
    val size = control.computeSize(SWT.DEFAULT, SWT.DEFAULT)
    scrolled.setMinSize(size.x, size.y)

    scrolled
  }

  import OverlayPreferenceStore._
  private def makeOverlayKeys(syntaxClass: IsabelleSyntaxClass): List[OverlayKey] = {
    List(
      new OverlayKey(STRING, syntaxClass.foregroundColorKey),
      new OverlayKey(BOOLEAN, syntaxClass.foregroundColorEnabledKey),
      new OverlayKey(STRING, syntaxClass.backgroundColorKey),
      new OverlayKey(BOOLEAN, syntaxClass.backgroundColorEnabledKey),
      new OverlayKey(BOOLEAN, syntaxClass.boldKey),
      new OverlayKey(BOOLEAN, syntaxClass.italicKey),
      new OverlayKey(BOOLEAN, syntaxClass.underlineKey),
      new OverlayKey(INT, syntaxClass.underlineStyleKey),
      new OverlayKey(BOOLEAN, syntaxClass.underlineStyleKey))
  }

  def makeOverlayPreferenceStore = {
    val keys = ALL_SYNTAX_CLASSES.flatMap(makeOverlayKeys)
    new OverlayPreferenceStore(getPreferenceStore, keys.toArray)
  }

  override def performOk() = {
    super.performOk()
    overlayStore.propagate()
//    IsabelleUIPlugin.plugin.savePluginPreferences()
    InstanceScope.INSTANCE.getNode(IsabelleUIPlugin.plugin.pluginId).flush()
    true
  }

  override def dispose() {
    overlayStore.stop()
    super.dispose()
  }

  override def performDefaults() {
    super.performDefaults()
    overlayStore.loadDefaults()
    handleSyntaxColorListSelection()
  }

  def createTreeViewer(editorComposite: Composite) {
    treeViewer = new TreeViewer(editorComposite, SWT.SINGLE | SWT.BORDER)

    treeViewer.setContentProvider(SyntaxColoringTreeContentAndLabelProvider)
    treeViewer.setLabelProvider(SyntaxColoringTreeContentAndLabelProvider)

    val verticalBar = Option(treeViewer.getControl.asInstanceOf[Scrollable].getVerticalBar)

    // scrollbars and tree indentation guess
    val widthHint = ALL_SYNTAX_CLASSES.map { syntaxClass =>
      convertWidthInCharsToPixels(syntaxClass.displayName.length)
    }.max + verticalBar.map { _.getSize.x * 3 }.getOrElse(0)

    treeViewer.getControl.setLayoutData(GridDataFactory.
      swtDefaults.
      align(SWT.BEGINNING, SWT.BEGINNING).
      grab(false, true).
      hint(widthHint, convertHeightInCharsToPixels(11)).
      create)

    treeViewer.addDoubleClickListener(new IDoubleClickListener {
      override def doubleClick(event: DoubleClickEvent) {
        val element = event.getSelection.asInstanceOf[IStructuredSelection].getFirstElement
        if (treeViewer.isExpandable(element))
          treeViewer.setExpandedState(element, !treeViewer.getExpandedState(element))        
      }
    })

    treeViewer.addSelectionChangedListener(new ISelectionChangedListener {
      override def selectionChanged(event: SelectionChangedEvent) = 
        handleSyntaxColorListSelection()
    })

    treeViewer.setInput(new Object)
  }

  def createSyntaxPage(parent: Composite): Control = {
    overlayStore.load()
    overlayStore.start()

    val outerComposite = new Composite(parent, SWT.NONE)
    outerComposite.setLayout(GridLayoutFactory.fillDefaults.create)

    val link = new Link(outerComposite, SWT.NONE)
    val msg = "Default colors and font can be configured on the " +
    		"<a href=\"org.eclipse.ui.preferencePages.GeneralTextEditor\">'Text Editors'</a> " +
    		"and on the " +
    		"<a href=\"org.eclipse.ui.preferencePages.ColorsAndFonts\">'Colors and Fonts'</a> " +
    		"preference page."
    link.setText(msg)
    link.addSelectionListener(new SelectionAdapter {
      override def widgetSelected(event: SelectionEvent) =
        PreferencesUtil.createPreferenceDialogOn(parent.getShell, event.text, null, null)
    })

    def fillHorizontal = GridDataFactory.swtDefaults.align(SWT.FILL, SWT.CENTER).grab(true, false)

    link.setLayoutData(fillHorizontal.hint(150, SWT.DEFAULT).create)

    val filler = new Label(outerComposite, SWT.LEFT)
    filler.setLayoutData(fillHorizontal.
        hint(SWT.DEFAULT, new PixelConverter(outerComposite).convertHeightInCharsToPixels(1) / 2).
        create)

    val elementLabel = new Label(outerComposite, SWT.LEFT)
    elementLabel.setText("&Element:")
    elementLabel.setLayoutData(fillHorizontal.create)

    val elementEditorComposite = new Composite(outerComposite, SWT.NONE)
    elementEditorComposite.setLayout(GridLayoutFactory.fillDefaults.numColumns(2).create)
    elementEditorComposite.setLayoutData(fillHorizontal.create)

    createTreeViewer(elementEditorComposite)

    val stylesComposite = new Composite(elementEditorComposite, SWT.NONE)
    stylesComposite.setLayout(GridLayoutFactory.fillDefaults.numColumns(2).create)
    stylesComposite.setLayoutData(GridDataFactory.fillDefaults.grab(true, true).create)

    // do not indent (Scala preferences had the 'enabled' button as well)
    def indented = GridDataFactory.swtDefaults//.indent(20, SWT.DEFAULT)
    def indented2 = indented.span(2, 1)

    foregroundColorEnabledCheckBox = new Button(stylesComposite, SWT.CHECK)
    foregroundColorEnabledCheckBox.setText("Foreground:")
    foregroundColorEnabledCheckBox.setLayoutData(indented.create)

    syntaxForegroundColorEditor = new ColorSelector(stylesComposite)
    foregroundColorButton = syntaxForegroundColorEditor.getButton
    foregroundColorButton.setLayoutData(GridDataFactory.swtDefaults.create)


    backgroundColorEnabledCheckBox = new Button(stylesComposite, SWT.CHECK)
    backgroundColorEnabledCheckBox.setText("Background:")
    backgroundColorEnabledCheckBox.setLayoutData(indented.create)

    syntaxBackgroundColorEditor = new ColorSelector(stylesComposite)
    backgroundColorButton = syntaxBackgroundColorEditor.getButton
    backgroundColorButton.setLayoutData(GridDataFactory.swtDefaults.create)


    boldCheckBox = new Button(stylesComposite, SWT.CHECK)
    boldCheckBox.setText("&Bold")

    boldCheckBox.setLayoutData(indented2.create)

    italicCheckBox = new Button(stylesComposite, SWT.CHECK)
    italicCheckBox.setText("&Italic")
    italicCheckBox.setLayoutData(indented2.create)

    underlineCheckBox = new Button(stylesComposite, SWT.CHECK)
    underlineCheckBox.setText("&Underline")
    underlineCheckBox.setLayoutData(indented2.create)

    val setJEditColorsButton = new Button(outerComposite, SWT.PUSH)
    setJEditColorsButton.setText("Set Isabelle/jEdit syntax colors")
    setJEditColorsButton.setLayoutData(GridDataFactory.swtDefaults.create)
    setJEditColorsButton.addSelectionListener { () => setJEditPreferences() }

    setUpSelectionListeners()

    treeViewer.setSelection(new StructuredSelection(isabelleCategory))

    outerComposite.layout(false)
    outerComposite
  }

  private def setUpSelectionListeners() {
    foregroundColorButton.addSelectionListener { () =>
      for (syntaxClass <- selectedSyntaxClass)
        PreferenceConverter.setValue(overlayStore, syntaxClass.foregroundColorKey, 
            syntaxForegroundColorEditor.getColorValue)
    }
    foregroundColorEnabledCheckBox.addSelectionListener { () =>
      for (syntaxClass <- selectedSyntaxClass) {
        overlayStore.setValue(syntaxClass.foregroundColorEnabledKey, 
            foregroundColorEnabledCheckBox.getSelection)
        foregroundColorButton.setEnabled(foregroundColorEnabledCheckBox.getSelection)
      }
    }
    backgroundColorButton.addSelectionListener { () =>
      for (syntaxClass <- selectedSyntaxClass)
        PreferenceConverter.setValue(overlayStore, syntaxClass.backgroundColorKey, 
            syntaxBackgroundColorEditor.getColorValue)
    }
    backgroundColorEnabledCheckBox.addSelectionListener { () =>
      for (syntaxClass <- selectedSyntaxClass) {
        overlayStore.setValue(syntaxClass.backgroundColorEnabledKey, 
            backgroundColorEnabledCheckBox.getSelection)
        backgroundColorButton.setEnabled(backgroundColorEnabledCheckBox.getSelection)
      }
    }
    boldCheckBox.addSelectionListener { () =>
      for (syntaxClass <- selectedSyntaxClass)
        overlayStore.setValue(syntaxClass.boldKey, boldCheckBox.getSelection)
    }
    italicCheckBox.addSelectionListener { () =>
      for (syntaxClass <- selectedSyntaxClass)
        overlayStore.setValue(syntaxClass.italicKey, italicCheckBox.getSelection)
    }
    underlineCheckBox.addSelectionListener { () =>
      for (syntaxClass <- selectedSyntaxClass)
        overlayStore.setValue(syntaxClass.underlineKey, underlineCheckBox.getSelection)
    }
  }
  
  private def selectedSyntaxClass: Option[IsabelleSyntaxClass] = condOpt(treeViewer.getSelection) {
    case SelectedItems(syntaxClass: IsabelleSyntaxClass) => syntaxClass
  }

  private def massSetEnablement(enabled: Boolean) = {
    val widgets = List(
      syntaxForegroundColorEditor.getButton,
      foregroundColorEnabledCheckBox,
      syntaxBackgroundColorEditor.getButton,
      backgroundColorEnabledCheckBox,
      boldCheckBox,
      italicCheckBox,
      underlineCheckBox)
    widgets foreach { _.setEnabled(enabled) }
  }

  private def handleSyntaxColorListSelection() = selectedSyntaxClass match {
    case None =>
      massSetEnablement(false)
    case Some(syntaxClass) =>
      import syntaxClass._
      syntaxForegroundColorEditor.setColorValue(
          PreferenceConverter.getColor(overlayStore, foregroundColorKey))
      val foregroundColorEnabled = overlayStore getBoolean foregroundColorEnabledKey
      foregroundColorEnabledCheckBox.setSelection(foregroundColorEnabled)
      syntaxBackgroundColorEditor.setColorValue(
          PreferenceConverter.getColor(overlayStore, backgroundColorKey))
      val backgroundColorEnabled = overlayStore getBoolean backgroundColorEnabledKey
      backgroundColorEnabledCheckBox.setSelection(backgroundColorEnabled)
      boldCheckBox.setSelection(overlayStore getBoolean boldKey)
      italicCheckBox.setSelection(overlayStore getBoolean italicKey)
      underlineCheckBox.setSelection(overlayStore getBoolean underlineKey)

      massSetEnablement(true)
      syntaxForegroundColorEditor.getButton.setEnabled(foregroundColorEnabled)
      syntaxBackgroundColorEditor.getButton.setEnabled(backgroundColorEnabled)
  }

  /**
   * Loads and sets Isabelle/jEdit syntax colours. 
   */
  private def setJEditPreferences() {
    ColorPreferenceInitializer.putSyntaxColoringPreferencesJEdit(overlayStore)
    handleSyntaxColorListSelection()
  }
  

  implicit def noArgFnToSelectionAdapter(p: () => Any): SelectionAdapter =
    new SelectionAdapter() {
      override def widgetSelected(e: SelectionEvent) { p() }
    }

  object SelectedItems {
    def unapplySeq(selection: ISelection): Option[List[Any]] = condOpt(selection) {
      case structuredSelection: IStructuredSelection => structuredSelection.toArray.toList
    }
  }

}

object SyntaxColoringTreeContentAndLabelProvider extends LabelProvider with ITreeContentProvider {

  def getElements(inputElement: AnyRef) = IsabelleSyntaxClasses.categories.toArray

  def getChildren(parentElement: AnyRef) = parentElement match {
    case Category(_, children) => children.toArray
    case _ => Array()
  }

  def getParent(element: AnyRef): Category =
    IsabelleSyntaxClasses.categories.find(_.children contains element).orNull

  def hasChildren(element: AnyRef) = getChildren(element).nonEmpty

  def inputChanged(viewer: Viewer, oldInput: AnyRef, newInput: AnyRef) {}

  override def getText(element: AnyRef) = element match {
    case Category(name, _) => name
    case IsabelleSyntaxClass(displayName, _) => displayName
  }
}
