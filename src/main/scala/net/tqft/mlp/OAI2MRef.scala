package net.tqft.mlp

import scala.io.Source
import java.io.File
import net.tqft.mathscinet.MRef
import net.tqft.toolkit.wiki.WikiMap
import net.tqft.toolkit.wiki.FirefoxDriver
import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import net.tqft.toolkit.Logging

object OAI2MRef extends App {

  lazy val arxivbot = {
    val b = WikiMap("http://tqft.net/mlp/index.php")
    b.login("arxivbot", "zytopex")
    b.enableSQLReads("jdbc:mysql://mysql.tqft.net/mathematicsliteratureproject?user=readonly1&password=readonly", "mlp_")
    b
  }

  import net.tqft.toolkit.collections.Split._

  var count = 0
  val input: File = new File("/Users/scott/projects/arxiv-toolkit/arxiv.txt")

  import scala.slick.driver.MySQLDriver.simple._

  SQL { implicit session =>
    val articlesWithoutMatchingDOI = for (
      a <- SQLTables.arxiv;
      if a.journalref.isNotNull;
      if a.doi.isNull || !SQLTables.mathscinet.filter(_.doi === a.doi).exists
    ) yield (a.arxivid, a.title, a.authors, a.journalref)

    for ((id, title, authorsXML, journalRef) <- articlesWithoutMatchingDOI) {
      try {
        val authors = (for (names <- (scala.xml.XML.loadString("<authors>" + authorsXML + "</authors>") \\ "author").iterator) yield (names \\ "keyname").text + ", " + (names \\ "forenames").text).mkString("", "; ", ";")
        val citation = (title + "\n" + authors + "\n" + journalRef).trim
        println("Looking up: " + citation)
        val result = MRef.lookup(citation)
        println("  found: " + result.map(_.identifierString))

        if (result.size == 1) {
          for (r <- result) {
            arxivbot("Data:" + r.identifierString + "/FreeURL") = "http://arxiv.org/abs/" + id
          }
        }
        count += 1
      } catch {
        case e: Exception => {
          Logging.warn("Exception while looking up article using MRef", e)
        }
      }
    }
  }

  println(count)

  FirefoxDriver.quit
}