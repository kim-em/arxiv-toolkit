package net.tqft.arxiv2

import net.tqft.citationsearch.CitationScore
import net.tqft.util.pandoc
import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import slick.jdbc.MySQLProfile.api._

object EncryptedSourcesApp extends App {
  val ids = SQL {
    (for (a <- SQLTables.arxiv; if a.categories.startsWith("math"); if a.arxivid.startsWith("0705")) yield a.arxivid)
  }

  for (id <- ids; if Sources(id).keysIterator.exists(_.endsWith(".cry"))) {
    println(id)
  }
}