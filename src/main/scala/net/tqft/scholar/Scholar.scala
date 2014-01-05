package net.tqft.scholar

import net.tqft.util.Slurp
import org.openqa.selenium.WebDriver
import net.tqft.toolkit.Logging
import org.openqa.selenium.By
import net.tqft.journals.ISSNs
import net.tqft.mathscinet.Search
import net.tqft.wiki.WikiMap

object Scholar extends App {

  private lazy val scholarbot = {
    val b = WikiMap("http://tqft.net/mlp/index.php")
    //    b.login("scholarbot", "zytopex")
    b
  }

  def fromDOI(doi: String) = {

    def driver = FirefoxDriver.driverInstance

    driver.get("http://scholar.google.com/scholar?q=http://dx.doi.org/" + doi)
    import scala.collection.JavaConverters._
    val links = driver.findElements(By.partialLinkText("versions")).asScala
    links.headOption.map(_.click)

    val arxivLinks = driver.findElements(By.cssSelector("a[href^=\"http://arxiv.org/\"]")).asScala
    val pdfLinks = driver.findElements(By.partialLinkText("[PDF]")).asScala
    (arxivLinks ++ pdfLinks).map(_.getAttribute("href"))
  }

  val journals = Iterator(ISSNs.`Advances in Mathematics`, ISSNs.`Discrete Mathematics`, ISSNs.`Annals of Mathematics`, ISSNs.`Algebraic & Geometric Topology`, ISSNs.`Geometric and Functional Analysis`)
  val years = 2013 to 2013

  def articles = for (j <- journals; y <- years; a <- Search.inJournalYear(j, y)) yield a

  for (a <- articles) {
    println(a.DOI)
    for (doi <- a.DOI) {
      println(scholarbot.get("Data:" + a.identifierString + "/FreeURL"))
      println(fromDOI(doi))
    }
  }

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