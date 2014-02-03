package net.tqft.mathscinet

import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import net.tqft.toolkit.Logging
import net.tqft.toolkit.Profiler
import java.io.PrintStream
import java.io.FileOutputStream

object CitationSearch extends App {
  import scala.slick.driver.MySQLDriver.simple._

  SQL { implicit session =>
    def articlesPage(k: Int) = {
      println("retrieving page " + k)
      (for (
      a <- SQLTables.mathscinet
    ) yield a).drop(k * 1000).take(1000).list
    }
    def articlesPaged = Iterator.from(0).map(articlesPage).takeWhile(_.nonEmpty).flatten

    val index = scala.collection.mutable.Map[String, scala.collection.mutable.Set[Int]]()
    
    val (t, _) = Profiler.timing(
      for (a <- articlesPaged) {
        try {
          val citation = a.constructFilename(maxLength = None).stripSuffix(".pdf")
          for (w <- citation.split(" ").map(_.stripPrefix("(").stripSuffix(")").stripSuffix(",").stripSuffix("."))) {
            index.getOrElseUpdate(w, scala.collection.mutable.Set[Int]()) += a.identifier
          }
        } catch {
          case e: Exception => Logging.warn("Exception while preparing citation for:\n" + a.bibtex.toBIBTEXString, e)
        }
      })

      println(t)
      
//    println(index.toSeq.sortBy(p => -p._2.size).map(p => (p._1, p._2.size)))
    println(index.size)
    import net.tqft.toolkit.collections.Tally._
//    println(index.values.map(_.size).tally.sorted)
    
    val out = new PrintStream(new FileOutputStream("terms"))
    for((term, documents) <- index) {
      out.println(term)
      out.println(documents.mkString(","))
    }
  }

}