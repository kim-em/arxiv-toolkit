package net.tqft.mlp

import net.tqft.mlp.sql.SQLTables
import scala.slick.driver.MySQLDriver.simple._
import net.tqft.mlp.sql.SQL
import net.tqft.util.Similarity

object CompareTitles extends App {

  SQL { implicit session =>
    val arxivTitles = (for(a <- SQLTables.arxiv) yield (a.arxivid, a.title)).run
    val mathscinetTitles = (for(a <- SQLTables.mathscinet) yield (a.MRNumber, a.title)).run
    
    val results = for((id, t1) <- arxivTitles; (mr, Some(t2)) <- mathscinetTitles; j = Similarity.jaroWinkler(t1, t2); if j > 0.9; s = Similarity.levenshtein(t1, t2); if s > 0.6) yield {
      println(id, " ", mr)
      println(t1)
      println(t2)
      println(s, j)
      
      (id, mr, s, j)
    }
    
    println(results.size)
    println(results.map(_._3).max)
    println(results.map(_._4).max)
  }

}