package net.tqft.webofscience

import net.tqft.scholar.Scholar
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
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.Keys
import org.jsoup.safety.Whitelist
import org.jsoup.nodes.Document.OutputSettings
import org.jsoup.nodes.Element

case class Citation(title: String, authors: List[String], citation: String, DOIOption: Option[String], accessionNumber: Option[String], PubMedIdOption: Option[String]) {
  override def toString = s"Citation(\n title = $title,\n authors = $authors,\n citation = $citation,\n DOI = $DOIOption,\n accessionNumber = $accessionNumber\n)"

  def authorsText = {
    import net.tqft.util.OxfordComma._
    authors.map(author => pandoc.latexToText(net.tqft.mathscinet.Author(0, author).firstNameLastName)).oxfordComma
  }

  def fullCitationWithoutIdentifier = title + " - " + authorsText + " - " + citation
  def fullCitation = title + " - " + authorsText + " - " + citation + (if (DOIOption.nonEmpty) " DOI:" + DOIOption.get else "") + (if (accessionNumber.nonEmpty) " WoS:" + accessionNumber.get else "")
  def fullCitation_html = fullCitationWithoutIdentifier + " - " + (if (DOIOption.nonEmpty) s" DOI:<a href='http://dx.doi.org/${DOIOption.get}'>${DOIOption.get}</a>" else "") + (accessionNumber match {
    case Some(a) => " WoS:<a href=\"" + Article(a).url + "\">" + a + "</a>"
    case None => ""
  })

  def mathSciNetMatches = net.tqft.citationsearch.Search.query(fullCitation).results
  def bestMathSciNetMatch = mathSciNetMatches.headOption.flatMap(_.citation.MRNumber).map(i => net.tqft.mathscinet.Article(i))
}

case class Article(accessionNumber: String) {
  def url = s"http://apps.webofknowledge.com/InboundService.do?product=WOS&UT=$accessionNumber&action=retrieve&mode=FullRecord"

  lazy val jsoup = {
    def load = Jsoup.parse(Slurp(url).mkString("\n"))
    val result = load
    try {
      result.select(".title value").text
      result.select("p.sourceTitle").first.text
      result.select("div.block-record-info-source-values").text
      result
    } catch {
      case e: Exception => {
        Logging.warn("Something went wrong while reading metadata for " + this + ", trying again.", e)
        Slurp -= url
        load
      }
    }
  }
  def title = {
    jsoup.select(".title value").text
  }
  def DOIOption: Option[String] = {
    jsoup.select(".FR_field").asScala.map(_.text).find(_.startsWith("DOI:")).map(_.stripPrefix("DOI:").trim)
  }
  def PubMedIdOption: Option[String] = {
    jsoup.select(".FR_field").asScala.map(_.text).find(_.startsWith("PubMed ID:")).map(_.stripPrefix("PubMedID:").trim)
  }
  def authors = {
    jsoup.select("""a[title="Find more records by this author"]""").asScala.map(_.text).toSeq
  }
  def authorsText = {
    import net.tqft.util.OxfordComma._
    authors.map(author => pandoc.latexToText(net.tqft.mathscinet.Author(0, author).firstNameLastName)).oxfordComma
  }
  def citation = jsoup.select("p.sourceTitle").first.text + ", " + jsoup.select("div.block-record-info-source-values").text

  def fullCitationWithoutIdentifier = {
    title + " - " + authorsText + " - " + citation
  }
  def fullCitation = fullCitationWithoutIdentifier + " - " + DOIOption.map(" DOI:" + _).getOrElse("") + " WoS:" + accessionNumber
  def fullCitation_html = fullCitationWithoutIdentifier + " - " + DOIOption.map(doi => s" DOI:<a href='https://dx.doi.org/$doi'>$doi</a>").getOrElse("") + s" WoS:<a href='$url'>$accessionNumber</a>"

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
          val head +: tail = sourceField.split("  ").toSeq
          (head + " " + tail.filter(!_.startsWith("DOI: ")).mkString(" "), tail.find(_.startsWith("DOI: ")).map(_.stripPrefix("DOI: ")))
        }
        val accessionNumber = map.get("Accession Number").map(_.stripPrefix("WOS:"))
        Citation(title, authors, citation, doi, accessionNumber, map.get("PubMed ID"))
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
        val records = scrape_printable_citations(true)
        SQL { implicit session =>
          SQLTables.webofscience_aux += ((accessionNumber, title, authorsText, citation, records.mkString("-----<<-->>-----"), DOIOption))
        }
        records
      }
    }

  }

  def scrape_printable_citations(retry: Boolean = true): Seq[String] = {
    val driver = FirefoxDriver.driverInstance
    driver.get("http://webofknowledge.com/")
    Thread.sleep(1000)

    driver.get(url)
    try {
      Thread.sleep(1000)
      val timesCitedLink = driver.findElement(By.partialLinkText("Times Cited"))

        val timesCited = timesCitedLink.getText.split(" ").head.toInt
        timesCitedLink.click()

        val results = ListBuffer[String]()
        def scrapePage {
          driver.findElement(By.name("formatForPrint")).click()
          Thread.sleep(500)

          driver.findElements(By.name("formatForPrint")).asScala.last.click()

          Thread.sleep(200)
          
          val currentWindow = driver.getWindowHandle
          // Switch to new window opened
          (driver.getWindowHandles.asScala - currentWindow).headOption.map(driver.switchTo().window)
          //        val result = driver.getPageSource
          Thread.sleep(2000)
          results ++= driver.findElements(By.cssSelector("table")).asScala.map(_.getText.trim).filter(_.startsWith("Record"))
          driver.close
          driver.switchTo().window(currentWindow)
          //        results += result
        }

        if (driver.findElements(By.className("paginationNextDisabled")).asScala.isEmpty) {
          new Select(driver.findElement(By.id("selectPageSize_.bottom"))).selectByVisibleText("50 per page")
        }

        scrapePage
        while (driver.findElements(By.className("paginationNextDisabled")).asScala.isEmpty) {
          driver.findElement(By.className("paginationNext")).click
          scrapePage
        }

        if (!retry || results.size == timesCited) {
          results.toSeq
        } else {
          println("Found a different number of records than I was expecting!")
          println(timesCited)
          println(results.mkString("\n-----\n"))
          scrape_printable_citations(false)
        }
    } catch {
      case e: org.openqa.selenium.NoSuchElementException => {
        if (driver.getPageSource.contains("""<span class="TCcountFR">0</span> Times Cited""")) {
          Seq.empty
        } else {
          ???
        }
      }

    }

  }
}

object Article {
  def fromDOI(doi: String): Option[Article] = {
    import net.tqft.mlp.sql.SQL
    import net.tqft.mlp.sql.SQLTables
    import scala.slick.driver.MySQLDriver.simple._
    val lookup = SQL { implicit session =>
      (for (
        a <- SQLTables.doi2webofscience;
        if a.doi === doi
      ) yield a.accessionNumber).run.headOption
    }
    lookup match {
      case Some("") => None
      case Some(accessionNumber) => Some(Article(accessionNumber))
      case None => {
        Scholar.fromDOI(doi).flatMap(r => r.webOfScienceAccessionNumber).orElse(searchForDOI(doi)) match {
          case Some(accessionNumber) => {
            SQL { implicit session => SQLTables.doi2webofscience += (doi, accessionNumber) }
            Some(Article(accessionNumber))
          }
          case None => {
            SQL { implicit session => SQLTables.doi2webofscience += (doi, "") }
            None
          }
        }
      }
    }
  }

  def searchForDOI(doi: String): Option[String] = {
    def driver = FirefoxDriver.driverInstance
    def jse = driver.asInstanceOf[JavascriptExecutor]

    try {
      driver.get("http://apps.webofknowledge.com/")

      driver.findElement(By.cssSelector("i.icon-dd-active-block-search")).click();
      driver.findElement(By.linkText("Advanced Search")).click();
      driver.findElement(By.id("value(input1)")).clear();
      driver.findElement(By.id("value(input1)")).sendKeys("DO=" + doi);
      driver.findElement(By.cssSelector("#searchButton > input[type=\"image\"]")).click();
      driver.findElement(By.linkText("1")).click();
      driver.findElement(By.cssSelector("value")).click();

      driver.findElements(By.className("FR_field")).asScala.map(_.getText).filter(_.startsWith("DOI:")).map(_.stripPrefix("DOI:").trim).headOption match {
        case Some(foundDOI) if foundDOI == doi => {
          driver.findElements(By.className("FR_field")).asScala.map(_.getText).filter(_.startsWith("Accession Number:")).map(_.stripPrefix("Accession Number:").trim.stripPrefix("WOS:")).headOption
        }
        case Some(badDOI) => {
          Logging.warn("Searching for DOI:" + doi + " on Web of Science lead to a page with DOI:" + badDOI)
          None
        }
        case None => {
          None
        }
      }
    } catch {
      case e: Exception => {
        Logging.warn("Exception while searching for DOI:" + doi + " on Web of Science.", e)
        None
      }
    }
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