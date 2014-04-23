package isabelle.eclipse.launch.tabs

import isabelle.eclipse.core.app.IsabelleBuild.IsabellePaths

/**
 * An observable value wrapper that combines available Isabelle installation path observables.
 *
 * @author Andrius Velykis
 */
class IsabellePathsObservableValue(path: ObservableValue[Option[String]],
                                   cygwinRootOpt: Option[ObservableValue[Option[String]]] = None)
    extends ObservableValue[Option[IsabellePaths]] {

  path.subscribe(publish)
  cygwinRootOpt foreach (_.subscribe(publish))

  override def value: Option[IsabellePaths] = cygwinRootOpt match {
    case None => path.value map { p => IsabellePaths(p) }
    // if cygwinRoot observable exist, both values must be present to combine
    case Some(cygwinRoot) => (path.value, cygwinRoot.value) match {
      case (Some(p), Some(root)) => Some(IsabellePaths(p, Some(root)))
      case _ => None
    }
  }

}
