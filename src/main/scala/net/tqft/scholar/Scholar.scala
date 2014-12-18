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

object Scholar {

  case class ScholarResults(arxivLinks: Seq[String], pdfURLs: Iterator[String], webOfScienceAccessionNumber: Option[String])

  def fromDOI(doi: String) = {
    def driver = FirefoxDriver.driverInstance

    try {
      driver.get("http://scholar.google.com/scholar?q=http://dx.doi.org/" + doi)

      println(driver.getCurrentUrl())

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
        for(link <- webOfScienceLinks.headOption;
            url = link.getAttribute("href");
            _ = { println(url); None };
            matches <- accessionNumberRegex.findFirstMatchIn(url)
        ) yield matches.group(1)
      
      Some(ScholarResults(arxivURLs, pdfURLs, webOfScienceAccessionNumber))
    } catch {
      case e: Exception => {
        Logging.warn("Exception while reading from Google Scholar", e)
        None
      }
    }
  }

}

