package net.tqft.mlp

import net.tqft.mlp.sql.SQLTables
import scala.slick.driver.MySQLDriver.simple._
import net.tqft.mlp.sql.SQL
import net.tqft.wiki.WikiMap

object DOIMatchBot extends App {

  lazy val arxivbot = {
    val b = WikiMap("http://tqft.net/mlp/index.php")
    b.login("arxivbot", "zytopex")
    b
  }

  SQL { implicit session =>
    val matchingDOIs = for {
      a <- SQLTables.arxiv;
      b <- SQLTables.mathscinet if a.doi === b.doi
    } yield (a.arxivid, b.MRNumber)

    println(matchingDOIs.selectStatement)

    println("Matching DOIs:")

    val results = matchingDOIs.run

    for ((arxivid, mrnumber) <- results) {

      println(arxivid + " " + mrnumber)
      arxivbot("Data:")
    }

    println("total: " + results.size)
  }
}