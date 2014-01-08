package net.tqft.scholar

import net.tqft.util.Slurp
import org.openqa.selenium.WebDriver
import net.tqft.toolkit.Logging
import org.openqa.selenium.By
import net.tqft.journals.ISSNs
import net.tqft.mathscinet.Search
import net.tqft.wiki.WikiMap
import net.tqft.util.Throttle
import net.tqft.util.PDF

object Scholar extends App {

  private lazy val scholarbot = {
    val b = WikiMap("http://tqft.net/mlp/index.php")
    b.login("scholarbot", "zytopex")
    b
  }

  def fromDOI(doi: String) = {
    def driver = FirefoxDriver.driverInstance

    driver.get("http://scholar.google.com/scholar?q=http://dx.doi.org/" + doi)

    println(driver.getCurrentUrl())

    while ({
      Thread.sleep(Throttle.logNormalDistribution(20000).toLong);
      driver.getCurrentUrl().contains("scholar.google.com/sorry/")
    }) {
      // Uhoh, we hit their captcha
      println("Oops, we've hit google's robot detector. Please kill this job, or go do the captcha.")
    }

    import scala.collection.JavaConverters._
    val links = driver.findElements(By.partialLinkText("versions")).asScala
    links.headOption.map(_.click)

    while ({
      Thread.sleep(Throttle.logNormalDistribution(5000).toLong);
      driver.getCurrentUrl().contains("scholar.google.com/sorry/")
    }) {
      // Uhoh, we hit their captcha
      println("Oops, we've hit google's robot detector. Please kill this job, or go do the captcha.")
    }

    val arxivLinks = driver.findElements(By.cssSelector("a[href^=\"http://arxiv.org/\"]")).asScala
    val pdfLinks = driver.findElements(By.partialLinkText("[PDF]")).asScala
    (arxivLinks.map(_.getAttribute("href")),
      pdfLinks.map(_.getAttribute("href")))
  }

  for (a <- net.tqft.mlp.extendedCoverage.toSeq.sortBy(x => scala.util.Random.nextDouble())) {
    println(a.DOI)
    for (doi <- a.DOI) {
      if (scholarbot.get("Data:" + a.identifierString + "/FreeURL").isEmpty) {
        println("searching...")
        val r = fromDOI(doi)
        for (link <- r._1.headOption) {
          println("posting arxiv link: " + link)
          scholarbot("Data:" + a.identifierString + "/FreeURL") = link
        }
        if (r._1.isEmpty && r._2.isEmpty) {
          println("none available")
          scholarbot("Data:" + a.identifierString + "/FreeURL") = "none available, according to Google Scholar"
        }
        if (r._1.isEmpty && r._2.nonEmpty) {
          for (link <- r._2.filter(PDF.getBytes(_).nonEmpty).headOption) {
            println("posting PDF link: " + link)
            scholarbot("Data:" + a.identifierString + "/FreeURL") = "Google Scholar suggests: " + link
          }
        }
        println("done")
      }
    }
  }

  net.tqft.wiki.FirefoxDriver.quit
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
    driverOption.map(_.quit)
    driverOption = None
  }

}
