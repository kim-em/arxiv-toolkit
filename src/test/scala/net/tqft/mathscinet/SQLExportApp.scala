package net.tqft.mathscinet

import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import net.tqft.toolkit.Logging
import java.io.PrintWriter
import java.io.FileOutputStream
import java.io.File

object SQLExportApp extends App {

  import slick.jdbc.MySQLProfile.api._
  import scala.concurrent.ExecutionContext.Implicits.global

//  val out = new PrintWriter(new FileOutputStream(new File("./mathscinet.bib")))

  for (a <- SQL.stream(SQLTables.mathscinet)) {
    println(a.bibtex.toBIBTEXString)
//    out.println(a.bibtex.toBIBTEXString)
//    out.println()
  }
}