package net.tqft.journals

import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver
import net.tqft.toolkit.Logging
import java.net.URL

object Process {
  lazy val driver: WebDriver = {
    Logging.info("Starting Firefox/webdriver")
    val result = new FirefoxDriver()
    Logging.info("   ... finished starting Firefox")
    result
  }

  def apply(doi: String) = {
    driver.get("http://dx.doi.org/" + doi)
    val url = driver.getCurrentUrl
    new URL(url).getHost match {
      case "???" => ???
      case _ => ???
    }
    
  }
}