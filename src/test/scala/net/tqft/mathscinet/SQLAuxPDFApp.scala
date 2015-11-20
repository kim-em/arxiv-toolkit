package net.tqft.mathscinet

import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import scala.slick.driver.MySQLDriver.simple._
import net.tqft.toolkit.Logging
import net.tqft.util.pandoc
import scala.collection.parallel.ForkJoinTaskSupport

object SQLAuxPDFApp extends App {

  SQL { implicit session =>
    def articlesPage(k: Int): List[(net.tqft.mathscinet.Article, (Int, String, String, String, String, String, String, Option[String], Option[String]))] = {
      try {
        println("retrieving page " + k)
        (for (
          a <- SQLTables.mathscinet;
          aux <- SQLTables.mathscinet_aux;
          if a.`type` === "article";
          if a.MRNumber === aux.MRNumber;
          if a.issn =!= "";
          if aux.pdf.isNull || aux.pdf === ""
        ) yield (a, aux)).drop(k * 1000).take(1000).list
      } catch {
        case e: Exception => {
          Logging.error("SQL exception while looking up mathscinet articles", e)
          Thread.sleep(60 * 1000)
          articlesPage(k)
        }
      }
    }

    var group = articlesPage(0)
    while (group.nonEmpty) {

      for ((a, aux) <- group) {
        try {
          val pdf = a.stablePDFURL.getOrElse("-")
          println("Adding PDF URL for " + a.identifierString + ": " + pdf)
          SQLTables.mathscinet_aux.filter(_.MRNumber === a.identifier).map(_.pdf).update(Some(pdf))
        } catch {
          case e: Exception => {
            Logging.error("Exception while inserting \n" + a.bibtex.toBIBTEXString, e)
            // throw e
          }
        }
      }
      group = articlesPage(0)

    }
  }
}