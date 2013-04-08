package net.tqft.util
import com.gargoylesoftware.htmlunit._
import com.gargoylesoftware.htmlunit.html._
import java.net.URL
import be.roam.hue.doj.Doj

object HTML {

  def apply(url: String) = {
    val response = new StringWebResponse(Slurp(url).mkString("\n"), new URL(url));
    val client = new WebClient()
    HTMLParser.parseHtml(response, client.getCurrentWindow());
  }
  
  def jQuery(url: String) = {
    Doj.on(apply(url))
  }

}