package net.tqft.portico

import java.net.URL
import net.tqft.util.Slurp
import net.tqft.util.FirefoxSlurp
import org.openqa.selenium.By
import scala.collection.JavaConversions
import net.tqft.citationsearch.Search

object Portico {
	def scrape(url: String) = {
	  val driver = FirefoxSlurp.driverInstance
	  driver.get(url)
	  Thread.sleep(1000)
	  import JavaConversions._
	  for(element <- driver.findElements(By.className("blurbLineHeightStyle")).toList) yield {
	    val List(title, author, citation, doi_, _, _, id_, _) = element.getText().split("\n").toList
	    val doi = doi_.stripPrefix("DOI: ")
	    val id = id_.stripPrefix("Portico Item ID: ")
	    (title, author, citation, doi, id, Search.goodMatch(title + " " + author + " " + citation + " " + doi))
	  }
	}
}