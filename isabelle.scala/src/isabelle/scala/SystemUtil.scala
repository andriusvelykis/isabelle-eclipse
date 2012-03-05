
package isabelle.scala

import scala.collection.JavaConversions._
import scala.io.Source
import isabelle.Path
import isabelle.Platform
import isabelle.Standard_System
import java.io.File

object SystemUtil extends isabelle.Basic_Library {
  
  /*
   * Copied from Isabelle_System.init()
   */
  def getLogics(path : String) : java.util.List[String] = {
    
    val standard_system = new Standard_System
    val settings =
    {
      val env = Map(System.getenv.toList: _*) +
        ("THIS_JAVA" -> standard_system.this_java())

      val isabelle_home =
        if (path != null) path
        else
          env.get("ISABELLE_HOME") match {
            case None | Some("") =>
              val path = System.getProperty("isabelle.home")
              if (path == null || path == "") error("Unknown Isabelle home directory")
              else path
            case Some(path) => path
          }

      Standard_System.with_tmp_file("settings") { dump =>
        val shell_prefix =
          if (Platform.is_windows) List(standard_system.platform_root + "\\bin\\bash", "-l")
          else Nil
        val cmdline =
          shell_prefix ::: List(isabelle_home + "/bin/isabelle", "getenv", "-d", dump.toString)
        val (output, rc) = Standard_System.raw_exec(null, env, true, cmdline: _*)
        if (rc != 0) error(output)

        val entries =
          for (entry <- Source.fromFile(dump).mkString split "\0" if entry != "") yield {
            val i = entry.indexOf('=')
            if (i <= 0) (entry -> "")
            else (entry.substring(0, i) -> entry.substring(i + 1))
          }
        Map(entries: _*) +
          ("HOME" -> System.getenv("HOME")) +
          ("PATH" -> System.getenv("PATH"))
      }
    }
    
    find_logics(standard_system, settings)
  }
  
    /* find logics */

  def find_logics(system: Standard_System, settings: Map[String, String]): List[String] =
  {
    val ml_ident = getenv_strict(settings, "ML_IDENTIFIER")
    val logics = new scala.collection.mutable.ListBuffer[String]
    for (dir <- Path.split(getenv_strict(settings, "ISABELLE_PATH"))) {
      val files = platform_file(system, dir + Path.explode(ml_ident)).listFiles()
      if (files != null) {
        for (file <- files if file.isFile) logics += file.getName
      }
    }
    logics.toList.sortWith(_ < _)
  }
  
  /* getenv */

  def getenv(settings: Map[String, String], name: String): String =
    settings.getOrElse(if (name == "HOME") "HOME_JVM" else name, "")

  def getenv_strict(settings: Map[String, String], name: String): String =
  {
    val value = getenv(settings, name)
    if (value != "") value else error("Undefined environment variable: " + name)
  }
  
  /* path specifications */

  def standard_path(path: Path): String = path.expand.implode

  def platform_path(system: Standard_System, path: Path): String = system.jvm_path(standard_path(path))
  def platform_file(system: Standard_System, path: Path): File = new File(platform_path(system, path))


}
