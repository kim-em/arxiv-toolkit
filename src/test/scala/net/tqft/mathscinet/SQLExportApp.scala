package net.tqft.mathscinet

import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import net.tqft.toolkit.Logging
import java.io.PrintWriter
import java.io.FileOutputStream
import java.io.File

object SQLExportApp extends App {

  import scala.slick.driver.MySQLDriver.simple._

  val out = new PrintWriter(new FileOutputStream(new File("./mathscinet.bib")))
  
  SQL { 
      for (a <- SQLTables.mathscinet) {
        out.println(a.bibtex.toBIBTEXString)
        out.println()
      }
  }
}