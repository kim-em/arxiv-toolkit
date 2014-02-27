package net.tqft.mathscinet

import scala.io.Source
import net.tqft.journals.Journals
import net.tqft.journals.Journal
import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables

object JournalsNotOnTheERAList extends App {

  def count(issn: String) = {
    import scala.slick.driver.MySQLDriver.simple._

    SQL { implicit session =>
      (for (
        a <- SQLTables.mathscinet;
        if a.issn === issn;
        if a.`type` === "article";
        if a.year >= "2012"
      ) yield a).length.run
    }

  }

  val ERA = Source.fromURL("http://tqft.net/math/ERA2015-issns.txt").getLines.toSeq
  for (
    (issn, name) <- (Journals.names -- ERA).toSeq.par
  //    c = count(issn)
  //    if c > 0
  ) {
    val c = count(issn)
    if (c >= 10) {
      println(issn + ", " + name + ", " + c)
    }
  }
}