package isabelle.eclipse.launch.config

import scala.reflect.runtime.universe._

import org.eclipse.core.runtime.CoreException
import org.eclipse.debug.core.{ILaunchConfiguration, ILaunchConfigurationWorkingCopy}

import isabelle.eclipse.launch.IsabelleLaunchPlugin

/**
 * Utilities for launch configurations.
 * 
 * @author Andrius Velykis
 */
object LaunchConfigUtil {

  def configValue[T: TypeTag](configuration: ILaunchConfiguration,
                              attributeName: String,
                              defaultValue: T): T =
    try {

      val res = typeOf[T] match {
        case t if t =:= typeOf[String] =>
          configuration.getAttribute(attributeName, defaultValue.asInstanceOf[String])
        case t if t =:= typeOf[Boolean] =>
          configuration.getAttribute(attributeName, defaultValue.asInstanceOf[Boolean])
        case t if t =:= typeOf[Int] =>
          configuration.getAttribute(attributeName, defaultValue.asInstanceOf[Int])
        case _ =>
          throw new UnsupportedOperationException("unsupported config type")
      }

      res.asInstanceOf[T]

    } catch {
      case ce: CoreException => {
        IsabelleLaunchPlugin.log("Error reading configuration", ce)
        // return the default
        defaultValue
      }
    }

  def setConfigValue(configuration: ILaunchConfigurationWorkingCopy,
                     attributeName: String,
                     value: Option[String]) =
    value match {
      case Some(value) =>
        configuration.setAttribute(attributeName, value)
      case None =>
        configuration.removeAttribute(attributeName)
    }
  
}
