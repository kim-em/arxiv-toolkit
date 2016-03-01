package net.tqft.mathscinet

import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import net.tqft.toolkit.amazon.S3

/**
 * @author scott
 */
object UpdatePreliminary extends App {
  import scala.slick.driver.MySQLDriver.simple._

  val preliminary = (SQL { 
    (for (a <- SQLTables.mathscinet; if a.mrclass === "Preliminary Data") yield a).run
  })

  def getArticle(id: Int): Article = {
    try {
      new Article { override val identifier = id }
    } catch {
      case e: Exception => {
        Thread.sleep(100000)
        getArticle(id)
      }
    }
  }

  for (a <- preliminary; if a.year <= 2015) {
    try {
      println(a)

      S3("www.ams.org.cache") -= "http://www.ams.org/mathscinet/search/publications.html?fmt=bibtex&pg1=MR&s1=" + a.identifier.toString
      SQL { 
        (for (a0 <- SQLTables.mathscinet; if a0.MRNumber === a.identifier) yield a0).update(getArticle(a.identifier))
      }
    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }

}