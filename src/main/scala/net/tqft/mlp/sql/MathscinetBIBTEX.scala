package net.tqft.mlp.sql

import scala.slick.driver.MySQLDriver.simple._
import net.tqft.mathscinet.Article
import net.tqft.util.BIBTEX
import java.sql.Date

class MathscinetAux(tag: Tag) extends Table[(Int, String, String, String, String, Option[String])](tag, "mathscinet_aux") {
 def MRNumber = column[Int]("MRNumber", O.PrimaryKey)
  def textTitle = column[String]("textTitle")
  def wikiTitle = column[String]("wikiTitle")
  def textAuthors = column[String]("textAuthors")
  def textCitation = column[String]("textCitation")
  def pdf = column[Option[String]]("pdf")
  def * = (MRNumber, textTitle, wikiTitle, textAuthors, textCitation, pdf)
  def citationData = (MRNumber, textTitle, wikiTitle, textAuthors, textCitation)
}

class MathscinetBIBTEX(tag: Tag) extends Table[Article](tag, "mathscinet_bibtex") {
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
  def * = (MRNumber, `type`, title, booktitle, author, editor, doi, url, journal, fjournal, issn, isbn, volume, issue, year, pages, mrclass, number, address, edition, publisher, series) <> (buildArticle, { a: Article => Some(a.sqlRow) })

  private def buildArticle(data: bibtexTuple): Article = {
    val bibtexData = (
      ("title" -> data._3) ::
      ("booktitle" -> data._4) ::
      ("author" -> data._5) ::
      ("editor" -> data._6) ::
      ("doi" -> data._7) ::
      ("url" -> data._8) ::
      ("journal" -> data._9) ::
      ("fjournal" -> data._10) ::
      ("issn" -> data._11) ::
      ("isbn" -> data._12) ::
      ("volume" -> data._13) ::
      ("issue" -> data._14) ::
      ("year" -> data._15) ::
      ("pages" -> data._16) ::
      ("mrclass" -> data._17) ::
      ("number" -> data._18) ::
      ("address" -> data._19) ::
      ("edition" -> data._20) ::
      ("publisher" -> data._21) ::
      ("series" -> data._22) ::
      Nil).collect({ case (k, Some(v)) => (k -> v) })

    val result = new Article {
      override val identifier = data._1
    }
    result.bibtexData = Some(BIBTEX(data._2, result.identifierString, bibtexData))
    result
  }
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
  object mathscinet_aux extends TableQuery(new MathscinetAux(_)) {
    def citationData = map(aux => aux.citationData)
  }

  def mathscinet(ISSNs: TraversableOnce[String], years: TraversableOnce[Int]): Iterator[Article] = {
    import scala.slick.driver.MySQLDriver.simple._
    SQL { implicit session =>
      for (j <- ISSNs.toIterator; y <- years.toIterator; a <- SQLTables.mathscinet.filter(_.issn === j).filter(_.year === y.toString).iterator) yield a
    }

  }
}

object SQL {
  def apply[A](closure: slick.driver.MySQLDriver.backend.Session => A): A = Database.forURL("jdbc:mysql://mysql.tqft.net/mathematicsliteratureproject?user=mathscinetbot&password=zytopex", driver = "com.mysql.jdbc.Driver") withSession closure
}