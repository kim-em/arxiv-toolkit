package net.tqft.arxiv2

import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import slick.jdbc.MySQLProfile.api._
import java.util.Date
import scala.collection.parallel.ForkJoinTaskSupport

object CompleteAuthorshipTable extends App {

  val targets = ((SQL { for (article <- SQLTables.arxiv) yield article.arxivid }).toSet -- (SQL { for (authorship <- SQLTables.arxivAuthorshipsByName) yield authorship.arxivid }) -- Seq("math/0611658")).par
  val pool = new ForkJoinTaskSupport(new java.util.concurrent.ForkJoinPool(100))
  targets.tasksupport = pool
  println(targets.size + " articles to work on ...")

  for (id <- targets) {
    println(new Date())
    println("http://arxiv.org/abs/" + id)
    val a = (SQL { SQLTables.arxiv.filter(_.arxivid === id).take(1) }).head
    println(a.title)
    for (author <- a.authors) {
      val matchingAuthorQuery = {
        val baseQuery = SQLTables.arxivAuthorNames.filter(x => x.keyname === author._1 && x.forenames === author._2)
        val suffixQuery = if (author._3.isEmpty) {
          baseQuery.filter(_.suffix.isEmpty)
        } else {
          baseQuery.filter(_.suffix === author._3.get)
        }
        (if (author._4.isEmpty) {
          suffixQuery.filter(_.affiliation.isEmpty)
        } else {
          suffixQuery.filter(_.affiliation === author._4.get)
        }).take(1)
      }
      SQL { matchingAuthorQuery }.headOption match {
        case None => {
          println("Adding author record for " + author)
          val newId = SQL { (SQLTables.arxivAuthorNames.insertView returning SQLTables.arxivAuthorNames.map(_.id)) += author }
          println("Adding authorship record for " + author)
          SQL { SQLTables.arxivAuthorshipsByName.insertView += ((a.identifier, newId)) }
        }
        case Some(Author(id, _, _, _, _)) => {
          println("Adding authorship record for " + author)
          SQL { SQLTables.arxivAuthorshipsByName.insertView += ((a.identifier, id)) }
        }
      }
    }
  }
}