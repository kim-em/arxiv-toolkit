package net.tqft.mathscinet

import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import slick.jdbc.MySQLProfile.api._
import net.tqft.toolkit.Logging
import net.tqft.util.pandoc
import scala.collection.parallel.ForkJoinTaskSupport

object SQLAuxApp extends App {

  val pool = new ForkJoinTaskSupport(new java.util.concurrent.ForkJoinPool(100))

  val seen = scala.collection.mutable.Set[Int]()

  def articlesPage(k: Int) = {
    println("retrieving page " + k)
    SQL {
      (for (
        a <- SQLTables.mathscinet;
        if !SQLTables.mathscinet_aux.filter(_.MRNumber === a.MRNumber).exists
      ) yield a).drop(k * 1000).take(1000)
    }
  }

  var group = articlesPage(0)
  while (group.nonEmpty) {
    println(group.size)
    for (a <- { val p = group.par; p.tasksupport = pool; p }) {
      try {
        val data = (a.identifier, a.textTitle, a.wikiTitle, a.authorsText, a.citation_text, a.citation_markdown, a.citation_html)
        SQL { SQLTables.mathscinet_aux.citationData += (data) }
        println(SQLTables.mathscinet_aux.citationData.forceInsertStatementFor(data) + ";")
      } catch {
        case e: com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException if e.getMessage().startsWith("Duplicate entry") => {
          println("skipping " + a.identifierString)
        }
        case e: Exception => {
          Logging.error("Exception while inserting \n" + a.bibtex.toBIBTEXString, e)
        }
      }
    }
    seen ++= group.map(_.identifier)
    group = articlesPage(0)
    group = group.filterNot(a => seen.contains(a.identifier))
  }

}