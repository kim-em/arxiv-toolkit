package net.tqft.mathscinet

import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import scala.slick.driver.MySQLDriver.simple._
import net.tqft.toolkit.Logging
import net.tqft.util.pandoc
import scala.collection.parallel.ForkJoinTaskSupport
import net.tqft.citationsearch.Search.Citation

object SQLMatchesApp extends App {

  SQL { implicit session =>
//    There's no good way to rebuild this table; if the citation search database is expanded, just clear this table and rebuild.
//    SQLTables.arxiv_mathscinet_matches.delete
    
    def page(k: Int) = (for (
      a <- SQLTables.arxiv;
      if !SQLTables.arxiv_mathscinet_matches.filter(_.arxivid === a.arxivid).exists
    ) yield (a.arxivid, a.title, a.authors, a.journalref.?)).drop(k * 100).take(100).iterator

    val articles = Iterator.from(0).map(page).takeWhile(_.nonEmpty).flatten
    
    for ((arxivid, title, authorsXML, journalref) <- articles) {
      val authors = (for (names <- (scala.xml.XML.loadString("<authors>" + authorsXML + "</authors>") \\ "author").iterator) yield (names \\ "keyname").text + ", " + (names \\ "forenames").text).mkString("", "; ", ";")
      val results = net.tqft.citationsearch.Search.query(title + " " + authors + " " + journalref.getOrElse(""))
      val shortResults = results.headOption.toList ++ results.tail.filter(_._2 > 0.5)
      for ((Citation(mrnumber, _, _, _, _, _, _), score) <- shortResults) {
        SQLTables.arxiv_mathscinet_matches += ((arxivid, mrnumber, score))
      }
    }
  }
}