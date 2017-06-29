package net.tqft.arxiv.tools

import net.tqft.mlp.sql._

object UnmatchedDOIs extends App {

  import slick.driver.MySQLDriver.api._

  val results = SQL { 
     for (
      a <- SQLTables.arxiv;
      if a.journalref.isDefined;
      if !SQLTables.mathscinet.filter(_.doi === a.doi).exists
    ) yield (a.title, a.authors, a.journalref)
    
  }
    
    println(results.size)
    
}