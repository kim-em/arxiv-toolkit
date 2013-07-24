package net.tqft.util

import java.io.PrintWriter
import scala.sys.process._

object pandoc {
  val pandocPath: String = "/opt/local/bin/pandoc"
  val pandocCommand = pandocPath + " -f latex -t plain --no-wrap"

  def latexToText(latex: String): String = {
    var result = new StringBuffer()
    val pio = new ProcessIO(
      in => { in.write(latex getBytes "UTF-8"); in.close },
      stdout => scala.io.Source.fromInputStream(stdout).getLines.foreach(result.append),
      _ => ())
    val p = pandocCommand.run(pio)
    require(p.exitValue == 0)
    result.toString
  }
}