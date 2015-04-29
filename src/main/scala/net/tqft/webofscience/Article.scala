package net.tqft.webofscience

import net.tqft.scholar.Scholar
import net.tqft.scholar.FirefoxDriver
import org.openqa.selenium.By
import scala.collection.mutable.ListBuffer
import org.openqa.selenium.support.ui.Select
import net.tqft.util.Html
import scala.collection.JavaConverters._
import net.tqft.util.pandoc
import net.tqft.toolkit.Logging
import net.tqft.util.Slurp
import com.gargoylesoftware.htmlunit.html.HtmlAnchor
import com.gargoylesoftware.htmlunit.html.HtmlElement
import org.jsoup.Jsoup

case class Citation(title: String, authors: List[String], citation: String, DOIOption: Option[String], accessionNumber: Option[String]) {
  override def toString = s"Citation(\n title = $title,\n authors = $authors,\n citation = $citation,\n DOI = $DOIOption,\n accessionNumber = $accessionNumber\n)"

  def authorsText = {
    import net.tqft.util.OxfordComma._
    authors.map(author => pandoc.latexToText(net.tqft.mathscinet.Author(0, author).firstNameLastName)).oxfordComma
  }

  def fullCitationWithoutIdentifier = title + " - " + authorsText + " - " + citation + (if (DOIOption.nonEmpty) " DOI:" + DOIOption.get else "")
  def fullCitation = title + " - " + authorsText + " - " + citation + (if (DOIOption.nonEmpty) " DOI:" + DOIOption.get else "") + (if (accessionNumber.nonEmpty) " WOS:" + accessionNumber.get else "")
  def fullCitation_html = fullCitationWithoutIdentifier + (accessionNumber match {
    case Some(a) => " - <a href=\"" + Article(a).url + "\">WOS:" + a + "</a>"
    case None => ""
  })

  def mathSciNetMatches = net.tqft.citationsearch.Search.query(fullCitation).results
  def bestMathSciNetMatch = mathSciNetMatches.headOption.flatMap(_.citation.MRNumber).map(i => net.tqft.mathscinet.Article(i))
}

case class Article(accessionNumber: String) {
  def url = s"http://apps.webofknowledge.com/InboundService.do?product=WOS&UT=$accessionNumber&action=retrieve&mode=FullRecord"

  lazy val page = Slurp(url).mkString("\n")
  lazy val jsoup = Jsoup.parse(page)
  def title = {
    jsoup.select(".title value").text
  }
  def DOIOption: Option[String] = {
    jsoup.select(".FR_field").asScala.map(_.text).find(_.startsWith("DOI:")).map(_.stripPrefix("DOI:").trim)
  }
  def authors = {
    jsoup.select("""a[title="Find more records by this author"]""").asScala.map(_.text).toSeq
  }
  def authorsText = {
    import net.tqft.util.OxfordComma._
    authors.map(author => pandoc.latexToText(net.tqft.mathscinet.Author(0, author).firstNameLastName)).oxfordComma
  }
  def citation = {
    jsoup.select("p.sourceTitle").first.text + ", " + jsoup.select("div.block-record-info-source-values").text
  }
  
  def fullCitationWithoutIdentifier = {
    title + " - " + authorsText + " - " + citation
  }
  def fullCitation = fullCitationWithoutIdentifier  + " - " + DOIOption.map(" DOI:" + _).getOrElse("") + " WOS:" + accessionNumber
  def fullCitation_html = fullCitationWithoutIdentifier  + " - " + DOIOption.map(doi => s" DOI:<a href='https://dx.doi.org/$doi'>$doi</a>").getOrElse("") + s" WOS:<a href='$url'>$accessionNumber</a>"
  
  lazy val citations: Seq[Citation] = {
    val authorRegex = ".*(\\(.*\\))".r

    for (
      record <- citations_records;
      map = record.split("\n").toSeq.groupBy(line => line.takeWhile(_ != ':')).map(p => (p._1, p._2.head.stripPrefix(p._1 + ": ")));
      title <- map.get("Title");
      authorsField <- map.get("Author(s)").orElse(map.get("Book Author(s)"));
      sourceField <- map.get("Source")
    ) yield {
      try {
        val authors = authorsField.split("; ").toList.collect({ case authorRegex(name) => name.stripPrefix("(").stripSuffix(")") })
        val (citation, doi) = {
          val head +: tail = sourceField.split("Â Â ").toSeq
          (head + " " + tail.filter(!_.startsWith("DOI: ")).map(_.split(":").tail.mkString(" ").trim).mkString(" "), tail.find(_.startsWith("DOI: ")).map(_.stripPrefix("DOI: ")))
        }
        val accessionNumber = map.get("Accession Number").map(_.stripPrefix("WOS:"))
        Citation(title, authors, citation, doi, accessionNumber)
      } catch {
        case e: Exception => {
          throw new Exception(e.getMessage() + "\n" + record)
        }
      }
    }
  }

  lazy val citations_records = {
    import net.tqft.mlp.sql.SQL
    import net.tqft.mlp.sql.SQLTables
    import scala.slick.driver.MySQLDriver.simple._
    val lookup = SQL { implicit session =>
      (for (a <- SQLTables.webofscience_aux; if a.accessionNumber === accessionNumber; if a.citations_records.isNotNull) yield {
        a.citations_records
      }).run.headOption.map(_.split("-----<<-->>-----").toSeq.map(_.trim).filter(_.nonEmpty))
    }
    lookup match {
      case Some(result) => result
      case None => {
        val records = for (
          page <- scrape_printable_citations;
          record <- Html.jQuery(Html.preloaded("http://dev.null/", page)).get("table").allElements().map(_.asText()).filter(_.startsWith("Record"))
        ) yield {
          record
        }
        SQL { implicit session =>
          SQLTables.webofscience_aux.citations_recordsView += ((accessionNumber, records.mkString("-----<<-->>-----")))
        }
        records
      }
    }

  }

  private def scrape_printable_citations = {

    val driver = FirefoxDriver.driverInstance
    driver.get("http://webofknowledge.com/")
    Thread.sleep(1000)

    driver.get(url)
    try {
      driver.findElement(By.partialLinkText("Times Cited")).click()

      val results = ListBuffer[String]()
      def scrapePage {
        driver.findElement(By.name("formatForPrint")).click()
        Thread.sleep(1000)

        driver.findElements(By.name("formatForPrint")).asScala.last.click()

        val currentWindow = driver.getWindowHandle
        // Switch to new window opened
        (driver.getWindowHandles.asScala - currentWindow).headOption.map(driver.switchTo().window)
        val result = driver.getPageSource
        driver.close
        driver.switchTo().window(currentWindow)
        results += result
      }

      if (driver.findElements(By.className("paginationNextDisabled")).asScala.isEmpty) {
        new Select(driver.findElement(By.id("selectPageSize_.bottom"))).selectByVisibleText("50 per page")
      }

      scrapePage
      while (driver.findElements(By.className("paginationNextDisabled")).asScala.isEmpty) {
        driver.findElement(By.className("paginationNext")).click
        scrapePage
      }

      results.toSeq
    } catch {
      case e: org.openqa.selenium.NoSuchElementException => {
        Logging.warn("No 'Times Cited' link found for " + url, e)
        Seq.empty
      }

    }

  }
}

object Article {
  def fromDOI(doi: String): Option[Article] = {
    Scholar.fromDOI(doi).flatMap(r => r.webOfScienceAccessionNumber).map(Article(_))
  }

  def fromMathSciNet(article: net.tqft.mathscinet.Article, useGoogleScholar: Boolean = false): Option[Article] = {
    // first see if we have a record in the database
    import net.tqft.mlp.sql.SQL
    import net.tqft.mlp.sql.SQLTables
    import scala.slick.driver.MySQLDriver.simple._
    val lookup = SQL { implicit session =>
      (for (
        a <- SQLTables.webofscience_mathscinet_matches;
        if a.identifier === article.identifier
      ) yield a.accessionNumber).run.headOption
    }
    lookup match {
      case Some(accessionNumber) => Some(Article(accessionNumber))
      case None => {
        // if not look one up from the DOI, if available
        article.DOI match {
          case Some(doi) if useGoogleScholar => fromDOI(doi) match {
            case Some(result) => {
              // stash the result in the database
              SQL { implicit session =>
                SQLTables.webofscience_mathscinet_matches.insertView += ((result.accessionNumber, article.identifier, "Google Scholar"))
              }
              Some(result)
            }
            case None => {
              // TODO we could try harder...
              None
            }
          }
          case _ => None
        }
      }
    }
  }
}