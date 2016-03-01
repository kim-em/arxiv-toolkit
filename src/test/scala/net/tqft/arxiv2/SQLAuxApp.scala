package net.tqft.arxiv2

import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import scala.slick.driver.MySQLDriver.simple._
import net.tqft.toolkit.Logging
import net.tqft.util.pandoc
import scala.collection.parallel.ForkJoinTaskSupport

object SQLAuxApp extends App {

  val pool = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(100))

  val seen = scala.collection.mutable.Set[String]()

  SQL { 
    def articlesPage(k: Int) = {
      println("retrieving page " + k)
      (for (
        a <- SQLTables.arxiv;
        if !SQLTables.arxiv_aux.filter(_.arxivid === a.arxivid).exists
      ) yield a).drop(k * 1000).take(1000).list
    }

    var group = articlesPage(0)
    while (group.nonEmpty) {
      println(group.size)
      for (a <- { val p = group.par; p.tasksupport = pool; p }) {
        try {
          val data = (a.identifier, a.textTitle, a.authorsText)
          SQLTables.arxiv_aux += (data)
          println(SQLTables.arxiv_aux.insertStatementFor(data) + ";")
        } catch {
          case e: com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException if e.getMessage().startsWith("Duplicate entry") => {
            println("skipping " + a.identifier)
          }
          case e: Exception => {
            Logging.error("Exception while inserting \n" + a, e)
          }
        }
      }
      seen ++= group.map(_.identifier)
      group = articlesPage(0)
      group = group.filterNot(a => seen.contains(a.identifier))
    }
  }
}