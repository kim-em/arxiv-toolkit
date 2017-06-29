package net.tqft.mathscinet

import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import slick.jdbc.MySQLProfile.api._
import net.tqft.toolkit.Logging
import net.tqft.util.pandoc
import scala.collection.parallel.ForkJoinTaskSupport
import net.tqft.mlp.sql.Wiki

object SQLAuxFreeApp extends App {

  val pages = SQL {
    val query = (for (
      p <- Wiki.Pages;
      if p.page_namespace === 100;
      if p.page_title like "%/FreeURL";
      r <- Wiki.Revisions;
      if r.rev_id === p.page_latest;
      t <- Wiki.Texts;
      if t.old_id === r.rev_text_id
    ) yield (p.page_title, t.old_text))

    //    println(query.)
    query
  }

  for ((title, text) <- pages) {
    val r = "(http://[^ \\n]*)".r
    val links = r.findAllMatchIn(text).map(_.group(1)).toSeq

    val freeLink = links.find(_.contains("arxiv.org")) match {
      case Some(arxivLink) => arxivLink.replace("/abs/", "/pdf/")
      case None            => links.headOption.getOrElse("-")
    }
    val id = title.take(9).drop(2).toInt
    val q = SQL {
      SQLTables.mathscinet_aux
        .filter(_.MRNumber === id)
        .map(_.free)
        .update(Some(freeLink))
    }

    println("Adding Free URL for " + title.take(9) + ": " + freeLink + " returned " + q)
  }

}