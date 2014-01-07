package net.tqft.mlp

import net.tqft.mlp.sql.SQLTables
import scala.slick.driver.MySQLDriver.simple._
import net.tqft.mlp.sql.SQL

object DOIMatchBot extends App {

  SQL { implicit session =>
    val matchingDOIs = for {
      a <- SQLTables.arxiv;
      b <- SQLTables.mathscinet if a.doi === b.doi
    } yield (a.arxivid, b.MRNumber)

    println(matchingDOIs.selectStatement)

    val matchingTitles = for {
      a <- SQLTables.arxiv;
      b <- SQLTables.mathscinet if a.title === b.title
    } yield (a.arxivid, b.MRNumber)

    println(matchingTitles.selectStatement)

    println("Matching DOIs:")
    for ((arxivid, mrnumber) <- matchingDOIs.run) {
      println(arxivid + " " + mrnumber)
    }
    println("Matching titles:")
    for ((arxivid, mrnumber) <- matchingTitles.run) {
      println(arxivid + " " + mrnumber)
    }
  }
}