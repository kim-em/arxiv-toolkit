package net.tqft.arxiv2

import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import slick.jdbc.MySQLProfile.api._

object ListProlificAuthors extends App {

  val authorsArticlesQuery = (for (
    author <- SQLTables.arxivAuthorNames;
    article <- SQLTables.arxivAuthorshipsByName;
    if (article.author_name_id === author.id)
  ) yield (author, article)).groupBy(_._1)

  val authorsNumberOfArticlesQuery = authorsArticlesQuery.map({
    case (author, articles) => (author, articles.length)
  }).sortBy(_._2.desc)

  for ((author, count) <- SQL { authorsNumberOfArticlesQuery.take(5) }) {
    println(author.fullName + " has " + count + " articles.")
  }

}