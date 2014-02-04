package net.tqft.mathscinet

import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import net.tqft.toolkit.Logging
import net.tqft.toolkit.Profiler
import java.io.PrintStream
import java.io.FileOutputStream

object SaveCitationsApp extends App {
  import scala.slick.driver.MySQLDriver.simple._

  SQL { implicit session =>
    def articlesPage(k: Int) = {
      println("retrieving page " + k)
      (for (
        a <- SQLTables.mathscinet
      ) yield a).drop(k * 1000).take(1000).list
    }
    def articlesPaged = Iterator.from(0).map(articlesPage).takeWhile(_.nonEmpty).flatten

    val out = new PrintStream(new FileOutputStream("cites"))

    for (a <- articlesPaged.take(10000)) {
      try {
        val cite = a.fullCitation
        out.println(a.identifierString)
        out.println(cite)
      } catch {
        case e: Exception => Logging.warn("Exception while preparing citation for:\n" + a.bibtex.toBIBTEXString, e)
      }
    }

  }

}