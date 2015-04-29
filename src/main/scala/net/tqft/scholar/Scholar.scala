package net.tqft.scholar

import net.tqft.util.Slurp
import org.openqa.selenium.WebDriver
import net.tqft.toolkit.Logging
import org.openqa.selenium.By
import net.tqft.journals.ISSNs
import net.tqft.mathscinet.Search
import net.tqft.toolkit.wiki.WikiMap
import net.tqft.util.Throttle
import net.tqft.util.PDF
import net.tqft.scopus
import net.tqft.webofscience

object Scholar {

  case class ScholarResults(query: String, title: String, cluster: String, webOfScienceAccessionNumber: Option[String], arxivLinks: Seq[String], pdfURLs: Iterator[String]) {
    def sqlRow = (query, title, cluster, webOfScienceAccessionNumber, arxivLinks.headOption.map(_.stripPrefix("http://arxiv.org/").stripPrefix("abs/").stripPrefix("pdf/")), pdfURLs.toStream.headOption)
  }

  def fromDOI(doi: String): Option[ScholarResults] = apply("http://dx.doi.org/" + doi)

  def apply(article: scopus.Article): Option[ScholarResults] = {
    article.DOIOption match {
      case Some(doi) => fromDOI(doi)
      case None => apply(article.title + ", " + article.authorsText)
    }
  }
  def apply(citation: webofscience.Citation): Option[ScholarResults] = {
    citation.DOIOption match {
      case Some(doi) => fromDOI(doi)
      case None => apply(citation.title + ", " + citation.authorsText)
    }
  }

  def apply(queryString: String): Option[ScholarResults] = {
    import net.tqft.mlp.sql.SQL
    import net.tqft.mlp.sql.SQLTables
    import scala.slick.driver.MySQLDriver.simple._

    SQL { implicit session =>
      (for (a <- SQLTables.scholar_queries; if a.query === queryString) yield {
        a
      }).run.headOption
    } match {
      case Some(result) => {
        if (result.title.isEmpty) {
          None
        } else {
          Some(result)
        }
      }
      case None => {
        val lookup = implementation(queryString)
        SQL {
          implicit session =>
            SQLTables.scholar_queries += (lookup match {
              case Some(result) => result
              case None => ScholarResults(queryString, "", "", None, Nil, Iterator.empty)
            })
        }
        lookup
      }
    }

  }

  private def implementation(queryString: String): Option[ScholarResults] = {
    def driver = FirefoxDriver.driverInstance

    try {
      driver.get("http://scholar.google.com/scholar?q=" + queryString)

      while ({
        Thread.sleep(Throttle.logNormalDistribution(60000).toLong);
        driver.getTitle.contains("Sorry") || driver.getCurrentUrl().contains("scholar.google.com/sorry/")
      }) {
        // Uhoh, we hit their captcha
        println("Oops, we've hit google's robot detector. Please kill this job, or be a nice human and do the captcha.")
      }

      import scala.collection.JavaConverters._
      val links = driver.findElements(By.partialLinkText("versions")).asScala
      links.headOption.map(_.click)

      while ({
        Thread.sleep(Throttle.logNormalDistribution(5000).toLong);
        driver.getTitle.contains("Sorry") || driver.getCurrentUrl().contains("scholar.google.com/sorry/")
      }) {
        // Uhoh, we hit their captcha
        println("Oops, we've hit google's robot detector. Please kill this job, or be a nice human and do the captcha.")
      }

      val title = driver.findElements(By.className("gs_rt")).asScala.head.getText.stripPrefix("[CITATION]").trim
      val cluster = "cluster=([0-9]*)&".r.findFirstMatchIn(driver.getCurrentUrl()).get.group(1)

      val arxivLinks = driver.findElements(By.cssSelector("a[href^=\"http://arxiv.org/\"]")).asScala.toSeq

      val arxivURLs: Seq[String] = arxivLinks.map(_.getAttribute("href"))
      val pdfLinks = driver.findElements(By.partialLinkText("[PDF]")).asScala.toIterator
      val pdfURLs: Iterator[String] = if (arxivURLs.isEmpty) {
        (for (
          link <- pdfLinks.map(_.getAttribute("href"));
          if !link.startsWith("ftp");
          if !link.startsWith("http://link.springer.com/");
          if !link.startsWith("http://www.jointmathematicsmeetings.org/");
          if !link.startsWith("http://www.researchgate.net/");
          bytes <- PDF.getBytes(link);
          if PDF.portrait_?(bytes)
        ) yield { link })
      } else { Iterator.empty }

      val webOfScienceLinks = driver.findElements(By.partialLinkText("Web of Science")).asScala.toSeq
      val accessionNumberRegex = "UT=([A-Z0-9]*)&".r
      val webOfScienceAccessionNumber =
        for (
          link <- webOfScienceLinks.headOption;
          url = link.getAttribute("href");
          //          _ = { println(url); None };
          matches <- accessionNumberRegex.findFirstMatchIn(url)
        ) yield matches.group(1)

      Some(ScholarResults(queryString, title, cluster, webOfScienceAccessionNumber, arxivURLs, pdfURLs))
    } catch {
      case e: Exception => {
        //        Logging.warn("Exception while reading from Google Scholar", e)
        None
      }
    }
  }

}

