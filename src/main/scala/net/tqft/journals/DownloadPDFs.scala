package net.tqft.journals

import net.tqft.mlp.sql.SQLTables
import net.tqft.mlp.sql.SQL
import net.tqft.util.Slurp
import net.tqft.mathscinet.Article
import net.tqft.util.FirefoxSlurp

object DownloadPDFs extends App {

  val issn = ISSNs.`Journal of K-Theory`
  val target = "/Volumes/Repository Backups/Journal of K-Theory/"

  for (a <- Journal(issn).articles) {
//    val b = if (a.DOI.isEmpty) {
//      println("Discarding cached BIBTEX")
//      Slurp -= a.bibtexURL
//      import slick.driver.MySQLDriver.api._
//      SQL { 
//        println("Deleting the database record for " + a.identifierString + ": " + a.title)
//        SQLTables.mathscinet.filter(_.MRNumber === a.identifier).delete
//      }
//      Article(a.identifier)
//    } else {
//      a
//    }
    a.savePDF(target)
  }
  
  FirefoxSlurp.quit
}