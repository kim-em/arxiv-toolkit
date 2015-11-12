package net.tqft.arxiv2

import net.tqft.citationsearch.CitationScore
import net.tqft.util.pandoc
import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import scala.slick.driver.MySQLDriver.simple._

object EncryptedSourcesApp extends App {
    val ids = SQL { implicit session =>
    (for (a <- SQLTables.arxiv; if a.categories.startsWith("math"); if a.arxivid.startsWith("0705")) yield a.arxivid).run
  }

   for(id <- ids; if Sources(id).keysIterator.exists(_.endsWith(".cry"))) {
     println(id)
   }
}