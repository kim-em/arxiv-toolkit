package net.tqft.mathscinet

import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import net.tqft.toolkit.amazon.S3

/**
 * @author scott
 */
object UpdateTheses extends App {
  import slick.driver.MySQLDriver.api._

  val thesesWithoutNote = (SQL { 
    (for (a <- SQLTables.mathscinet; if a.publisher === "ProQuest LLC, Ann Arbor, MI"; if a.note.isEmpty) yield a)
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

  println("There are " + thesesWithoutNote.size + " theses which need updating.")
  
  for (a <- thesesWithoutNote) {
    try {
      println(a)

      S3("www.ams.org.cache") -= "http://www.ams.org/mathscinet/search/publications.html?fmt=bibtex&pg1=MR&s1=" + a.identifier.toString
      SQL { 
        (for (a0 <- SQLTables.mathscinet; if a0.MRNumber === a.identifier) yield a0).update(getArticle(a.identifier))
      }
      
      a.saveAux
    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }

}