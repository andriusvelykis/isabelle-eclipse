package isabelle.eclipse.ui.perspective

import org.eclipse.ui.{IPageLayout, IPerspectiveFactory}

import isabelle.eclipse.ui.internal.IsabelleUIPlugin.plugin


/**
 * Perspective definition for Isabelle theory.
 * 
 * @author Andrius Velykis
 */
class IsabelleTheoryPerspective extends IPerspectiveFactory {

  private def navigatorFolderId = plugin.pluginId + ".navigatorFolder"
  private def theoriesFolderId = plugin.pluginId + ".theoriesFolder"
  private def outputFolderId = plugin.pluginId + ".outputFolder"
  private def outlineFolderId = plugin.pluginId + ".outlineFolder"
  private def symbolsFolderId = plugin.pluginId + ".symbolsFolder"

  private def searchViewId = "org.eclipse.search.ui.views.SearchView"
  private def consoleViewId = "org.eclipse.ui.console.ConsoleView"
  private def navigatorViewId = "org.eclipse.ui.views.ResourceNavigator"
  private def logViewId = "org.eclipse.pde.runtime.LogView"
  
  private def outputViewId = "isabelle.eclipse.ui.proverOutputView"
  private def symbolsViewId = "isabelle.eclipse.ui.symbolsView"
  private def theoriesViewId = "isabelle.eclipse.ui.theoriesView"

  override def createInitialLayout(layout: IPageLayout) {
    val editorArea = layout.getEditorArea

    // put project explorer on the left
    val navFolder = layout.createFolder(navigatorFolderId, IPageLayout.LEFT, 0.2f, editorArea)
    navFolder.addView(IPageLayout.ID_PROJECT_EXPLORER)
    navFolder.addPlaceholder(navigatorViewId)

    // will put theories view below the navigator
    val theoriesFolder = layout.createFolder(
      theoriesFolderId, IPageLayout.BOTTOM, 0.5f, navigatorFolderId)
    theoriesFolder.addView(theoriesViewId)

    // put outline on the right
    val outlineFolder = layout.createFolder(outlineFolderId, IPageLayout.RIGHT, 0.75f, editorArea)
    outlineFolder.addView(IPageLayout.ID_OUTLINE)

    // will put symbols view below the outline
    val symbolsFolder = layout.createFolder(
      symbolsFolderId, IPageLayout.BOTTOM, 0.6f, outlineFolderId)
    symbolsFolder.addView(symbolsViewId)

    // put the prover output view on the bottom with various IDE views
    val outputFolder = layout.createFolder(outputFolderId, IPageLayout.BOTTOM, 0.75f, editorArea)
    outputFolder.addView(outputViewId)
    outputFolder.addView(IPageLayout.ID_PROBLEM_VIEW)
    outputFolder.addPlaceholder(IPageLayout.ID_TASK_LIST)
    outputFolder.addView(consoleViewId)
    outputFolder.addPlaceholder(searchViewId)
    outputFolder.addPlaceholder(logViewId)
    outputFolder.addPlaceholder(IPageLayout.ID_BOOKMARKS)
    outputFolder.addPlaceholder(IPageLayout.ID_PROGRESS_VIEW)

    // Add action sets
    layout.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET)

    // views - Isabelle
    layout.addShowViewShortcut(outputViewId)
    layout.addShowViewShortcut(theoriesViewId)
    layout.addShowViewShortcut(symbolsViewId)

    // views - search
    layout.addShowViewShortcut(searchViewId)

    // views - debugging
    layout.addShowViewShortcut(consoleViewId)

    // views - standard workbench
    layout.addShowViewShortcut(IPageLayout.ID_BOOKMARKS)
    layout.addShowViewShortcut(IPageLayout.ID_OUTLINE)
    layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW)
//    layout.addShowViewShortcut(IPageLayout.ID_TASK_LIST)
    layout.addShowViewShortcut(IPageLayout.ID_PROJECT_EXPLORER)
    layout.addShowViewShortcut(logViewId)

    // new actions - Java project creation wizard
    layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.folder")
    layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.file")
    layout.addNewWizardShortcut("org.eclipse.ui.editors.wizards.UntitledTextFileWizard")
  }
  
}
