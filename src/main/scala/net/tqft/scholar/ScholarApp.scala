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

object ScholarApp extends App {
  private lazy val scholarbot = {
    val b = WikiMap("http://tqft.net/mlp/index.php")
    b.login("scholarbot", "zytopex")
    b
  }

  for (
    g <- (net.tqft.mlp.extendedCoverage ++ net.tqft.mlp.topJournals(100)).grouped(1000);
    a <- scala.util.Random.shuffle(g)
  ) {
    println(a.DOI)
    for (doi <- a.DOI) {
      if (scholarbot.get("Data:" + a.identifierString + "/FreeURL").isEmpty) {
        println("searching...")
        for (Scholar.ScholarResults(_, _, _, _, arxivLinks, pdfURLs) <- Scholar.fromDOI(doi)) {
          for (link <- arxivLinks.headOption) {
            println("posting arxiv link: " + link)
            scholarbot("Data:" + a.identifierString + "/FreeURL") = link
          }
          if (arxivLinks.isEmpty && pdfURLs.isEmpty) {
            println("none available")
            scholarbot("Data:" + a.identifierString + "/FreeURL") = "none available, according to Google Scholar"
          }
          if (arxivLinks.isEmpty && pdfURLs.nonEmpty) {
            val link = pdfURLs.next
            println("posting PDF link: " + link)
            scholarbot("Data:" + a.identifierString + "/FreeURL") = "Google Scholar suggests: " + link
          }
          println("done")
        }
      } else {
        println("already done")
      }
    }
  }

  net.tqft.toolkit.wiki.FirefoxDriver.quit
  FirefoxDriver.quit
}

object FirefoxDriver {
  private var driverOption: Option[WebDriver] = None

  def driverInstance = {
    if (driverOption.isEmpty) {
      Logging.info("Starting Firefox/webdriver")
      driverOption = Some(new org.openqa.selenium.firefox.FirefoxDriver( /*profile*/ ))
      Logging.info("   ... finished starting Firefox")
    }
    driverOption.get
  }

  def quit = {
    try {
      driverOption.map(_.quit)
    } catch {
      case e: Exception => Logging.error("Exception occurred while trying to quit Firefox:", e)
    }
    driverOption = None
  }

}
