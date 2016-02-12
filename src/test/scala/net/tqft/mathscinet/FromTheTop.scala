package net.tqft.mathscinet

import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import net.tqft.toolkit.amazon.S3
import net.tqft.toolkit.Logging

/**
 * @author scott
 */
object FromTheTop extends App {
  import scala.slick.driver.MySQLDriver.simple._

  val max = (SQL { implicit session =>
    (for (a <- SQLTables.mathscinet) yield a.MRNumber).max.run
  }).get

  val gaps = (SQL { implicit session => 
    new scala.collection.mutable.BitSet(4000000) ++= (for (a <- SQLTables.mathscinet_gaps) yield a.MRNumber).iterator
  })
  
  println("gaps: " + gaps.size)
  
  for (i <- (3397000 to 3000000 by -1); if !Articles.identifiersInDatabase.contains(i); if !gaps.contains(i)) {
    try {
      Article(i)
    } catch {
      case e: NoSuchElementException => try {
        Logging.info(s"Recording that $i is not a valid MR number.")
        (SQL { implicit session => SQLTables.mathscinet_gaps += (i) })
      } catch {
        case e: Throwable => e.printStackTrace()
      }
      case e: Throwable => e.printStackTrace()
    }
  }

}