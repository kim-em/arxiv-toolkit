package net.tqft.arxiv2

import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import scala.slick.driver.MySQLDriver.simple._
import java.util.Date

object CompleteAuthorshipTable extends App {

  val articles = SQL {  (for (
        (article, authorship) <- SQLTables.arxiv joinLeft SQLTables.arxivAuthorshipsByName on (_.arxivid === _.arxivid);
        if(authorship.isEmpty)
      ) yield article).take(1000).selectStatement }

  println(articles)
  
  
  
      println(articles.size + " articles to work on ...")
  
//  var touched = true
//
//  while (touched) {
//    touched = false
//    SQL { 
//      val articles = (for (
//        article <- SQLTables.arxiv;
//        if !SQLTables.arxivAuthorshipsByName.filter(_.arxivid === article.arxivid).exists
//      ) yield article).take(1000).run
//
//      println(articles.size + " articles to work on ...")
//      
//      for (a <- articles) {
//        touched = true
//        session.withTransaction {
//          println(new Date())
//          println("http://arxiv.org/abs/" + a.identifier)
//          println(a.title)
//          for (author <- a.authors) {
//            val matchingAuthorQuery = {
//              val baseQuery = SQLTables.arxivAuthorNames.filter(x => x.keyname === author._1 && x.forenames === author._2)
//              val suffixQuery = if (author._3.isEmpty) {
//                baseQuery.filter(_.suffix.isNull)
//              } else {
//                baseQuery.filter(_.suffix === author._3.get)
//              }
//              if (author._4.isEmpty) {
//                suffixQuery.filter(_.affiliation.isNull)
//              } else {
//                suffixQuery.filter(_.affiliation === author._4.get)
//              }
//            }
//            matchingAuthorQuery.firstOption match {
//              case None => {
//                println("Adding author record for " + author)
//                val newId = (SQLTables.arxivAuthorNames.insertView returning SQLTables.arxivAuthorNames.map(_.id)) += author
//                println("Adding authorship record for " + author)
//                SQLTables.arxivAuthorshipsByName.insertView += ((a.identifier, newId))
//              }
//              case Some(Author(id, _, _, _, _)) => {
//                println("Adding authorship record for " + author)
//                SQLTables.arxivAuthorshipsByName.insertView += ((a.identifier, id))
//              }
//            }
//          }
//        }
//      }
//    }
//  }
}