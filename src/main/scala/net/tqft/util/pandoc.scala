package net.tqft.util

import java.io.PrintWriter
import scala.sys.process._
import java.io.File
import net.tqft.toolkit.Logging

object pandoc {
  val pandocPath: String = {
    val devPath = System.getProperty("user.home") + "/.cabal/bin/pandoc"
    if (new File(devPath).exists()) {
      Logging.info("Using development version of pandoc.")
      devPath
    } else {
      "/opt/local/bin/pandoc"
    }
  }
  val pandocCommand = pandocPath + " -f latex -t plain --no-wrap"

  private val okay = new File(pandocPath).exists()
  if (!okay) {
    Logging.warn("pandoc not found; not attempting to strip LaTeX")
  }

  def latexToText(latex: String): String = {
    if (okay) {
      var result = new StringBuffer()
      val pio = new ProcessIO(
        in => { in.write(latex getBytes "UTF-8"); in.close },
        stdout => scala.io.Source.fromInputStream(stdout).getLines.foreach(result.append),
        _ => ())
      val p = pandocCommand.run(pio)
      require(p.exitValue == 0, "pandoc failed on input: " + latex)
      result.toString
    } else {
      latex
    }
  }
}