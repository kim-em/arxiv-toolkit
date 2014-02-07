package net.tqft.mathscinet

import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import scala.slick.driver.MySQLDriver.simple._
import net.tqft.toolkit.Logging
import net.tqft.util.pandoc
import scala.collection.parallel.ForkJoinTaskSupport

object SQLAuxFreeApp extends App {

  val _tablePrefix = "mlp_"

  private class Revision(tag: Tag, tablePrefix: String) extends Table[(Int, Int)](tag, tablePrefix + "revision") {
    def rev_id = column[Int]("rev_id", O.PrimaryKey)
    def rev_text_id = column[Int]("rev_text_id")
    def * = (rev_id, rev_text_id)
  }
  private class Text(tag: Tag, tablePrefix: String) extends Table[(Int, String)](tag, tablePrefix + "text") {
    def old_id = column[Int]("old_id", O.PrimaryKey)
    def old_text = column[String]("old_text")
    def * = (old_id, old_text)
  }
  private class Page(tag: Tag, tablePrefix: String) extends Table[(Int, Int, String, Int)](tag, tablePrefix + "page") {
    def page_id = column[Int]("page_id", O.PrimaryKey)
    def page_namespace = column[Int]("page_namespace")
    def page_title = column[String]("page_title")
    def page_latest = column[Int]("page_latest")
    def * = (page_id, page_namespace, page_title, page_latest)
    def name_title = index("name_title", (page_namespace, page_title), unique = true)
  }

  private def Revisions = TableQuery(tag => new Revision(tag, _tablePrefix))
  private def Texts = TableQuery(tag => new Text(tag, _tablePrefix))
  private def Pages = TableQuery(tag => new Page(tag, _tablePrefix))

  val pages = SQL { implicit session =>
    val query = (for (
      p <- Pages;
      if p.page_namespace === 100;
      if p.page_title like "%/FreeURL";
      r <- Revisions;
      if r.rev_id === p.page_latest;
      t <- Texts;
      if t.old_id === r.rev_text_id
    ) yield (p.page_title, t.old_text))

    println(query.selectStatement)
    query.list
  }

  SQL { implicit session =>
    for ((title, text) <- pages) {
      val r = "(http://[^ \\n]*)".r
      val links = r.findAllMatchIn(text).map(_.group(1)).toSeq

      val freeLink = links.find(_.contains("arxiv.org")) match {
        case Some(arxivLink) => arxivLink.replace("/abs/", "/pdf/")
        case None => links.headOption.getOrElse("-")
      }
      val id = title.take(9).drop(2).toInt
      val q = SQLTables.mathscinet_aux
        .filter(_.MRNumber === id)
        .map(_.free)
        .update(Some(freeLink))

      println("Adding Free URL for " + title.take(9) + ": " + freeLink)       
    }

  }
}