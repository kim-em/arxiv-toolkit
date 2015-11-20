package net.tqft.mathscinet

import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import net.tqft.toolkit.Logging
import net.tqft.toolkit.amazon.AnonymousS3
import scala.collection.parallel.ForkJoinTaskSupport

object SQLImportApp extends App {

  import scala.slick.driver.MySQLDriver.simple._

  val pool = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(100))

  val cache = AnonymousS3("LoM-bibtex")


  SQL { implicit session =>
      for (group <- cache.keysIterator.toSeq.sorted.reverse.grouped(1000); groupPar = { val p = group.par; p.tasksupport = pool; p }; k <- groupPar; a = Article(k)) {
        println("inserting: " + a.identifierString)
        try {
          SQLTables.mathscinet += a
        } catch {
          case e: com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException if e.getMessage().startsWith("Duplicate entry") => {}
          case e: Exception => {
            Logging.error("Exception while inserting \n" + a.bibtex.toBIBTEXString, e)
//            throw e
          }
        }
      }
  }
}