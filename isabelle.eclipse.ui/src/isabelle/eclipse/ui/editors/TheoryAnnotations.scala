package isabelle.eclipse.ui.editors

import scala.actors.Actor._

import isabelle.Session
import isabelle.eclipse.core.util.{LoggingActor, SessionEvents}

/**
 * Updater for Isabelle theory editor annotations: tracks changes from the prover,
 * creates annotations and triggers update jobs.
 *
 * @author Andrius Velykis
 */
class TheoryAnnotations(editor: TheoryEditor)
    extends TheoryViewerAnnotations(
      editor.isabelleModel map (_.snapshot),
      editor.document,
      editor.annotationModel,
      Option(EditorUtil.getResource(editor.getEditorInput)),
      Option(editor.getSite.getWorkbenchWindow.getWorkbench.getDisplay))
    with SessionEvents {

  // When commands change (e.g. results from the prover), update the annotations accordingly.
  /** Subscribe to commands change session events */
  override protected def sessionEvents(session: Session) = List(session.commands_changed)
  
  /** When the session is initialised, update all annotations from scratch */
  override protected def sessionInit(session: Session) = updateAnnotations()
  
  /** The actor to react to session events */
  override protected val sessionActor = LoggingActor {
    loop {
      react {
        case changed: Session.Commands_Changed => {

          editor.isabelleModel foreach { model =>
            
            // avoid updating annotations if commands are from a different document
            if (changed.nodes contains model.name) {
              updateAnnotations(changed.commands)
            }
          }
        }
      }
    }
  }
  
  def init() {
    initSessionEvents()
  }
  
  def dispose() {
    disposeSessionEvents()
  }
    
}
