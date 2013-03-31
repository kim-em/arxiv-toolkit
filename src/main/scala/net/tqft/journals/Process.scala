package net.tqft.journals

import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver
import net.tqft.toolkit.Logging
import java.net.URL
import org.apache.commons.io.FileUtils
import java.io.File
import org.openqa.selenium.By

object Process extends Logging {
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
      case "link.springer.com" => {
        driver.findElement(By.id("action-bar-download-pdf-link")).click()
      }
      case _ => ???
    }
    if(driver.getPageSource.startsWith("%PDF")) {
      // oh, goody!
      info("found PDF for DOI:" + doi)
      saveCurrentPage(doi)
      true
    } else {
      info("no PDF found for DOI:" + doi)
      false
    }

  }

  def saveCurrentPage(filename: String) {
    FileUtils.writeStringToFile(new File(filename), driver.getPageSource)
  }
}