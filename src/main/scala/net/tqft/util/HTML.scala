package net.tqft.util
import com.gargoylesoftware.htmlunit._
import com.gargoylesoftware.htmlunit.html._
import java.net.URL
import be.roam.hue.doj.Doj

trait Html {

  def client = {
    val result = new WebClient(BrowserVersion.FIREFOX_31)
    result.getOptions.setThrowExceptionOnFailingStatusCode(false);
    result.getOptions.setThrowExceptionOnScriptError(false)
    result
  }
  
  def apply(url: String): HtmlPage = {
    client.getPage(url)
  }

  def jQuery(url: String) = {
    Doj.on(apply(url))
  }

  def jQuery(page: HtmlPage) = Doj.on(page)
  
  def preloaded(url: String, content: String): HtmlPage = {
    val response = new StringWebResponse(content, new URL(url));
    HTMLParser.parseHtml(response, client.getCurrentWindow());
  }
}

trait HtmlWithForeignSlurper extends Html {
  protected def slurp: Slurp = HtmlUnitSlurp

  override def apply(url: String) = {
    val response = new StringWebResponse(slurp(url).mkString("\n"), new URL(url));
    HTMLParser.parseHtml(response, client.getCurrentWindow());
  }
}

object Html extends Html {
  def usingSlurp(slurp: Slurp): Html = {
    val _slurp = slurp
    new HtmlWithForeignSlurper {
      override val slurp = _slurp
    }
  }
}