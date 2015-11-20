package net.tqft.sciencedirect

import net.tqft.util.FirefoxSlurp
import org.openqa.selenium.By

object ScienceDirect {
  lazy val preload = {
    val driver = FirefoxSlurp.driverInstance
    driver.get("http://www.sciencedirect.com/")
    if (driver.getTitle.contains("Choose Organization")) {
      driver.findElement(By.cssSelector("""input[name="dept"]""")).click() // select something
      driver.findElement(By.cssSelector("""input[value="51993"]""")).click() // if we're at ANU, use that
      driver.findElement(By.id("rememberOrg")).click()
      driver.findElement(By.cssSelector("""input[value="Continue"]""")).click()
    }
  }
}