package net.tqft.arxiv2

import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import slick.driver.MySQLDriver.api._
import net.tqft.toolkit.Logging
import net.tqft.util.pandoc
import scala.collection.parallel.ForkJoinTaskSupport

object SQLAuxApp extends App {

  val pool = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(100))

  val seen = scala.collection.mutable.Set[String]()

  def articlesPage(k: Int) = SQL {
    println("retrieving page " + k)
    (for (
      a <- SQLTables.arxiv;
      if !SQLTables.arxiv_aux.filter(_.arxivid === a.arxivid).exists
    ) yield a).drop(k * 1000).take(1000)
  }

  val targets = ((SQL { SQLTables.arxiv.map(_.arxivid) }).toSet -- (SQL { SQLTables.arxiv_aux.map(_.arxivid) })).par
  targets.tasksupport = pool

  for (id <- targets; a <- SQL { SQLTables.arxiv.filter(_.arxivid === id) }) {
    try {
      val data = (a.identifier, a.textTitle, a.authorsText)
      SQL { SQLTables.arxiv_aux += (data) }
      println(SQLTables.arxiv_aux.forceInsertStatementFor(data) + ";")
    } catch {
      case e: com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException if e.getMessage().startsWith("Duplicate entry") => {
        println("skipping " + a.identifier)
      }
      case e: Exception => {
        Logging.error("Exception while inserting \n" + a, e)
      }
    }
  }

}