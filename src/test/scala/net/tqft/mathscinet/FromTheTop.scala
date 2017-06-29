package net.tqft.mathscinet

import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import net.tqft.toolkit.amazon.S3
import net.tqft.toolkit.Logging

/**
 * @author scott
 */
object FromTheTop extends App {
  import slick.jdbc.MySQLProfile.api._

  val gaps = (
    new scala.collection.mutable.BitSet(4000000) ++= SQL {
      (for (a <- SQLTables.mathscinet_gaps) yield a.MRNumber)
    })

  println("gaps: " + gaps.size)

  for (i <- (3397000 to 3000000 by -1); if !Articles.identifiersInDatabase.contains(i); if !gaps.contains(i)) {
    try {
      Article(i)
    } catch {
      case e: NoSuchElementException => try {
        Logging.info(s"Recording that $i is not a valid MR number.")
        (SQL { SQLTables.mathscinet_gaps += (i) })
      } catch {
        case e: com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException => {}
        case e: Throwable => e.printStackTrace()
      }
      case e: Throwable => {
        e.printStackTrace()
        if (e.getMessage.startsWith("500")) {
          Logging.warn("Sleeping for 20 mins")
          Thread.sleep(1200000L)
        }
      }
    }
  }

}