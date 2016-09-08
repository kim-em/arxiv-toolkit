package net.tqft.webofscience

import net.tqft.util.FirefoxSlurp
import org.openqa.selenium.WebDriver
import net.tqft.toolkit.Logging

object WebOfScience {
  private var latch = true
  lazy val preload = {
    //  use FirefoxSlurp directly, to avoid the cache on this first hit.
    if (latch) {
      latch = false
      (new FirefoxSlurp {})("http://apps.webofknowledge.com/")
    }
    None
  }
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
