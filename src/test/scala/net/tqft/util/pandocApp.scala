package net.tqft.util

import java.io.File
import java.io.PrintStream
import java.io.FileOutputStream

object pandocApp extends App {
  println(pandoc.latexToText("""On some series containing {$\psi(x)-\psi(y)$} and {$(\psi(x)-\psi(y))^2$} for certain values of {$x$} and {$y$}. \v{C}\'{e}\"{o}"""))

  {
    val fos = new PrintStream(new FileOutputStream(new File(System.getProperty("user.home") + "/filenäme1")))
    fos.println("filenäme1")
    fos.close
  }
  {
    println(pandoc.latexToText("""filen\"{a}me2"""))
    val fos = new PrintStream(new FileOutputStream(new File(System.getProperty("user.home") + "/" + pandoc.latexToText("""filen\"{a}me2"""))))
    fos.println(pandoc.latexToText("""filen\"{a}me2"""))
    fos.close
  }
}