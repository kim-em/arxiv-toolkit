package net.tqft.mathscinet

import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import scala.slick.driver.MySQLDriver.simple._
import net.tqft.toolkit.Logging
import net.tqft.util.pandoc
import scala.collection.parallel.ForkJoinTaskSupport

object SQLAuxApp extends App {

  val pool = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(100))

  SQL { implicit session =>
    def articlesPage(k: Int) = {
      println("retrieving page " + k)
      (for (
        a <- SQLTables.mathscinet;
        if !SQLTables.mathscinet_aux.filter(_.MRNumber === a.MRNumber).exists
      ) yield a).drop(k * 1000).take(1000).list
    }

    var group = articlesPage(1)
    while (group.nonEmpty) {

      for (a <- group) {
        try {
          SQLTables.mathscinet_aux += ((a.identifier, a.textTitle, a.wikiTitle, a.authors.map(a => pandoc.latexToText(a.name)).mkString(" and "), pandoc.latexToText(a.citation)))
          println("inserted data for " + a.identifierString)
        } catch {
          case e: com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException if e.getMessage().startsWith("Duplicate entry") => {
            println("skipping " + a.identifierString)
          }
          case e: Exception => {
            Logging.warn("Exception while inserting \n" + a.bibtex.toBIBTEXString)
          }
        }
      }
      group = articlesPage(1)

    }
  }
}