package net.tqft.mathscinet

import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables

object SearchForArxivPapersOnMathSciNet extends App {
  import scala.slick.driver.MySQLDriver.simple._

  SQL { implicit session =>
    val articlesWithoutPublicationData = for (
      a <- SQLTables.arxiv;
      if a.journalref.isNull;
      if a.doi.isNull
    ) yield (a.arxivid, a.title, a.authors)

    for ((id, title, authorsXML) <- articlesWithoutPublicationData.take(100).list) {
      
      val authors = (for (names <- (scala.xml.XML.loadString("<authors>" + authorsXML + "</authors>") \\ "author").iterator) yield (names \\ "keyname").text + ", " + (names \\ "forenames").text).mkString("", "; ", ";")
      val query = title + " - " + authors
      println(s"Querying ($id): $query")
      val cites = net.tqft.citationsearch.Search.query(query)
    }

  }
}