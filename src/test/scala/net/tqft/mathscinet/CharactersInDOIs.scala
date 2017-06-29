package net.tqft.mathscinet

import net.tqft.mlp.sql._

object CharactersInDOIs extends App {
  import slick.jdbc.MySQLProfile.api._
  import scala.concurrent.ExecutionContext.Implicits.global

  val characters = scala.collection.mutable.Set[Char]()
  
  for (a <- SQL { SQLTables.mathscinet }; doi <- a.DOI) {
    characters ++= doi
    println(characters.toList.sorted)
  }
}