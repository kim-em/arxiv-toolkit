package net.tqft.util

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
  val pandocHTMLCommand = pandocPath + " -f latex -t html --no-wrap"

  private val okay = new File(pandocPath).exists()
  if (!okay) {
    Logging.warn("pandoc not found; not attempting to strip LaTeX")
  }

  // {On {W}alkup's class {$\scr K(d)$} and a minimal triangulation of {$(S3\mathop{\hbox{$}\times{$}\!\!\!\!\lower 3pt\hbox{--}}\ S1)^{\#3}$}}

  def latexToText(latex: String, retry: Boolean = true): String = {
    impl(pandocCommand)(latex, retry)
  }
  
  def latexToHTML(latex: String, retry: Boolean = true): String = {
    impl(pandocHTMLCommand)(latex, retry)
  }
  
  private def impl(pandocCommand: String)(latex: String, retry: Boolean = true): String = {
    if (okay) {
      try {
        var result = new StringBuffer()
        val pio = new ProcessIO(
          in => { in.write(latex getBytes "UTF-8"); in.close },
          stdout => scala.io.Source.fromInputStream(stdout).getLines.foreach(result.append),
          _ => ())
        val p = pandocCommand.run(pio)
        if (p.exitValue != 0) {
          Logging.warn("pandoc failed on input: " + latex)
          latex
        } else {
          result.toString
        }
      } catch {
        case e: Exception if retry => {
          impl(pandocCommand)(latex.replaceAllLiterally("{", "").replaceAllLiterally("}", ""), false)
        }
      }
    } else {
      latex
    }
  }
}