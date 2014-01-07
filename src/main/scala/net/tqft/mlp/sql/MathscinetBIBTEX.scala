package net.tqft.mlp.sql

import scala.slick.driver.MySQLDriver.simple._
import java.sql.Date
import scala.slick.model.PrimaryKey

class MathscinetBIBTEX(tag: Tag) extends Table[(Int, String, Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String])](tag, "mathscinet_bibtex") {
  def MRNumber = column[Int]("MRNumber", O.PrimaryKey)
  def `type` = column[String]("type")
  def title = column[Option[String]]("title")
  def booktitle = column[Option[String]]("booktitle")
  def author = column[Option[String]]("author")
  def editor = column[Option[String]]("editor")
  def doi = column[Option[String]]("doi")
  def url = column[Option[String]]("url")
  def journal = column[Option[String]]("journal")
  def fjournal = column[Option[String]]("fjournal")
  def issn = column[Option[String]]("issn")
  def isbn = column[Option[String]]("isbn")
  def volume = column[Option[String]]("volume")
  def issue = column[Option[String]]("issue")
  def year = column[Option[String]]("year")
  def pages = column[Option[String]]("pages")
  def mrclass = column[Option[String]]("mrclass")
  def number = column[Option[String]]("number")
  def address = column[Option[String]]("address")
  def edition = column[Option[String]]("edition")
  def publisher = column[Option[String]]("publisher")
  def series = column[Option[String]]("series")
  def * = (MRNumber, `type`, title, booktitle, author, editor, doi, url, journal, fjournal, issn, isbn, volume, issue, year, pages, mrclass, number, address, edition, publisher, series)
  
}

class Arxiv(tag: Tag) extends Table[(String, Date, Date, String, String, String, String, String, String, String, String, String, String, String, String)](tag, "arxiv") {
  def arxivid = column[String]("arxivid", O.PrimaryKey)
  def created = column[Date]("created")
  def updated = column[Date]("updated")
  def authors = column[String]("authors")
  def title = column[String]("title")
  def categories = column[String]("categories")
  def comments = column[String]("comments")
  def proxy = column[String]("proxy")
  def reportno = column[String]("reportno")
  def mscclass = column[String]("mscclass")
  def acmclass = column[String]("acmclass")
  def journalref = column[String]("journalref")
  def doi = column[String]("doi")
  def license = column[String]("license")
  def `abstract` = column[String]("abstract")
  def * = (arxivid, created, updated, authors, title, categories, comments, proxy, reportno, mscclass, acmclass, journalref, doi, license, `abstract`)
}

object SQLTables {
  val mathscinet = TableQuery[MathscinetBIBTEX]
  val arxiv = TableQuery[Arxiv]
}

object SQL {
  def apply[A](closure: slick.driver.MySQLDriver.backend.Session => A): A = Database.forURL("jdbc:mysql://mysql.tqft.net/mathematicsliteratureproject?user=mathscinetbot&password=zytopex", driver = "com.mysql.jdbc.Driver") withSession closure  
}