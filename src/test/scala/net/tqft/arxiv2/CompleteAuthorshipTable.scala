package net.tqft.arxiv2

import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import scala.slick.driver.MySQLDriver.simple._
import java.util.Date

object CompleteAuthorshipTable extends App {

  var touched = true

  while (touched) {
    touched = false
    SQL { implicit session =>
      val articles = (for (
        article <- SQLTables.arxiv;
        if !SQLTables.arxivAuthorshipsByName.filter(_.arxiv_id === article.arxivid).exists
      ) yield article).take(1000)

      for (a <- articles) {
        touched = true
        session.withTransaction {
          println(new Date())
          println(a.title)
          for (author <- a.authors) {
            val matchingAuthorQuery = {
              val baseQuery = SQLTables.arxivAuthorNames.filter(x => x.keyname === author._1 && x.forenames === author._2)
              val suffixQuery = if (author._3.isEmpty) {
                baseQuery.filter(_.suffix.isNull)
              } else {
                baseQuery.filter(_.suffix === author._3.get)
              }
              if (author._4.isEmpty) {
                suffixQuery.filter(_.affiliation.isNull)
              } else {
                suffixQuery.filter(_.affiliation === author._4.get)
              }
            }
            matchingAuthorQuery.firstOption match {
              case None => {
                println("Adding author record for " + author)
                val newId = (SQLTables.arxivAuthorNames.insertView returning SQLTables.arxivAuthorNames.map(_.id)) += author
                println("Adding authorship record for " + author)
                SQLTables.arxivAuthorshipsByName.insertView += ((a.identifier, newId))
              }
              case Some(Author(id, _, _, _, _)) => {
                println("Adding authorship record for " + author)
                SQLTables.arxivAuthorshipsByName.insertView += ((a.identifier, id))
              }
            }
          }
        }
      }
    }
  }
}