package net.tqft.mega

import org.openqa.selenium.WebDriver
import net.tqft.toolkit.Logging
import org.openqa.selenium.firefox.FirefoxProfile
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.By

object Mega extends Logging {

  lazy val driver: WebDriver = {
    Logging.info("Starting Firefox/webdriver")
    val profile = new FirefoxProfile();
    profile.setPreference("pdfjs.disabled", true);
    val result = new FirefoxDriver(profile)
    Logging.info("   ... finished starting Firefox")
    result
  }

  def executor = driver.asInstanceOf[JavascriptExecutor]

  def test {
    driver.get("https://mega.co.nz/")

//  ExpectedConditions.presenceOfElementLocated(By.id("myDynamicElement"))
    executor.executeScript("if ($('#seleniumUpload').length == 0) { seleniumUpload = window.$('<input/>').attr({id: 'seleniumUpload', type:'file'}).appendTo('body'); }")
    val uploadElement = driver.findElement(By.id("seleniumUpload"))
    uploadElement.sendKeys("~/foo")
    executor.executeScript("fileList = [ seleniumUpload.get(0).files[0] ]; e = $.Event('drop'); e.originalEvent = { dataTransfer : { files : fileList } }; $('#pageholder').trigger(e);")
  }
}