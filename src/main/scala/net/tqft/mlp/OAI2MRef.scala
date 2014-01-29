package net.tqft.mlp

import scala.io.Source
import java.io.File
import net.tqft.mathscinet.MRef
import net.tqft.toolkit.collections.Split.splittableIterator
import net.tqft.wiki.WikiMap
import net.tqft.wiki.FirefoxDriver
import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables

object OAI2MRef extends App {

  lazy val arxivbot = {
    val b = WikiMap("http://tqft.net/mlp/index.php")
    b.login("arxivbot", "zytopex")
    b
  }

  import net.tqft.toolkit.collections.Split._

  var count = 0
  val journals = scala.collection.mutable.Map[String, String]()
  val input: File = new File("/Users/scott/projects/arxiv-toolkit/arxiv.txt")

  import scala.slick.driver.MySQLDriver.simple._

  SQL { implicit session =>
    val articlesWithoutMatchingDOI = for (
      a <- SQLTables.arxiv;
      if a.journalref.isNotNull;
      if a.doi.isNull || !SQLTables.mathscinet.filter(_.doi === a.doi).exists
    ) yield (a.arxivid, a.title, a.authors, a.journalref)

    for ((id, title, authorsXML, journalRef) <- articlesWithoutMatchingDOI) {
      println(authorsXML)
      val authors = (for(names <- (scala.xml.XML.loadString("<authors>" + authorsXML + "</authors>") \\ "author").iterator) yield  (names \\ "keyname").text + ", " + (names \\ "forenames").text).mkString("", "; ", ";")
      val citation = (title + "\n" + authors + "\n" + journalRef).trim
      println("Looking up: " + citation)
      val result = MRef.lookup(citation)
      println("  found: " + result.map(_.identifierString))

      if (result.size == 1) {
        for (r <- result) {
          if (!journals.contains(r.journal)) {
            println("New journal: " + r.journal + " ---> " + r.ISSN)
            journals(r.journal) = r.ISSN
          }
          arxivbot("Data:" + r.identifierString + "/FreeURL") = "http://arxiv.org/abs/" + id
        }
      }
      count += 1

    }
  }

  println(count)
  println(journals)

  FirefoxDriver.quit
}