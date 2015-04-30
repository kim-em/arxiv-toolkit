package net.tqft.scopus

import net.tqft.util.Slurp
import net.tqft.util.FirefoxSlurp
import net.tqft.toolkit.Logging
import net.tqft.scholar.FirefoxDriver
import scala.collection.mutable.ListBuffer
import org.openqa.selenium.By
import scala.collection.JavaConverters._

object Scopus {
  private var latch = true
  lazy val preload = {
    //  use FirefoxSlurp directly, to avoid the cache on this first hit.
    if (latch) {
      latch = false
      (new FirefoxSlurp {})("http://www.scopus.com/")
    }
    None
  }
}

case class Author(id: Long, name: String, email: Option[String] = None) {
  def url = "http://www.scopus.com/authid/detail.url?authorId=" + id.toString

  lazy val publications: Seq[Article] = {
    import net.tqft.mlp.sql.SQL
    import net.tqft.mlp.sql.SQLTables
    import scala.slick.driver.MySQLDriver.simple._
    val lookup = SQL { implicit session =>
      (for (a <- SQLTables.scopus_authorships; if a.author_id === id) yield {
        a.scopus_id
      }).run
    }
    
    (if (lookup.isEmpty) {
      val r = """<a onclick="return submitRecord\('(.*)','[0-9]*','[0-9]*'\);" title="Show document details" href=".*">(.*)</a>""".r
      val lines = publications_results
//      println(lines.mkString("\n"))
      val records = (for (line <- lines; if line.contains("Show document details"); m <- r.findAllMatchIn(line)) yield {
        Article(m.group(1), Some(m.group(2)))
      }).toStream.distinct

      SQL { implicit session =>
        for (a <- records) {
          SQLTables.scopus_authorships += ((id, a.id))
        }
      }
      records.map(_.id)
    } else {
      lookup
    }).map(a => Article(a))
  }

  def publications_results: Seq[String] = {
    val driver = FirefoxDriver.driverInstance
    driver.get("http://www.scopus.com/")

    driver.get(url)
    Thread.sleep(1000)

    try {
      driver.findElement(By.partialLinkText("in search results format")).click
      val results = ListBuffer[String]()
      def scrapePage {
        Thread.sleep(1000)
        results ++= driver.getPageSource.split("\n")
      }

      scrapePage

      var nextLink = driver.findElements(By.cssSelector("a[title='Next page']")).asScala.headOption.filter(_.getAttribute("href") != null)
      while (nextLink.nonEmpty) {
        nextLink.get.click
        nextLink = driver.findElements(By.cssSelector("a[title='Next page']")).asScala.headOption.filter(_.getAttribute("href") != null)
        scrapePage
      }

      results.toSeq
    } catch {
      case e: org.openqa.selenium.NoSuchElementException => {
        throw new Exception("could not locate element with link text 'in search results format' on page " + url, e)
      }
    }

  }

  //    def publicationsURL(page: Int)  = "http://www.scopus.com/search/submit/author.url?authorId=" + id.toString + (if (page > 0) { "&offset=" + (20 * page + 1) } else { "" })
  ////  def publicationsURL(page: Int) = "http://www.scopus.com/results/results.url?sort=plf-f&src=s&sot=aut&sdt=a&sl=17&s=AU-ID%28" + id.toString + "%29" + (if (page > 0) { "&offset=" + (20 * page + 1) } else { "" })
  //
  //  lazy val publications: Stream[Article] = {
  //    //    <a onclick="javascript:submitRecord('2-s2.0-70349820748','9','5');" title="Show document details" href="http://www.scopus.com/record/display.url?eid=2-s2.0-70349820748&amp;origin=resultslist&amp;sort=plf-f&amp;src=s&amp;sid=6995D3618DB9D6D6ACB543BC41D0E039.FZg2ODcJC9ArCe8WOZPvA%3a20&amp;sot=aut&amp;sdt=a&amp;sl=35&amp;s=AU-ID%28%22Morrison%2c+Scott%22+7201672329%29&amp;relpos=9&amp;relpos=9&amp;citeCnt=5&amp;searchTerm=AU-ID%28%5C%26quot%3BMorrison%2C+Scott%5C%26quot%3B+7201672329%29">Skein theory for the D2 n planar algebras</a>
  //
  //    val r = """<a onclick="return:submitRecord\('(.*)','[0-9]*','[0-9]*'\);" title="Show document details" href=".*">(.*)</a>""".r
  //
  ////    val r = """title="Show document details" onClick="return:submitRecord\('(.*)','0','0'\);">(.*)</a>""".r
  ////    val r = """<a onclick="javascript:submitRecord\('(.*)','[0-9]*','[0-9]*'\);" title="Show document details" href=".*">(.*)</a>""".r
  //
  //    val firstPage = Slurp(publicationsURL(0)).toStream
  //
  //    val totalArticleCount = """value="([0-9]*)" name="count"""".r.findFirstMatchIn(firstPage.mkString("\n")).map(_.group(1)).get.toInt
  //    val allPages = for (i <- (0 until (totalArticleCount + 19) / 20).toStream; line <- Slurp(publicationsURL(i))) yield line
  //
  //    val result = (for (line <- allPages; if line.contains("Show document details"); m <- r.findAllMatchIn(line)) yield {
  //      Article(m.group(1), Some(m.group(2)))
  //    }).toStream.distinct
  //    if (result.size != totalArticleCount) {
  //      println(allPages.mkString("\n"))
  //      Logging.warn("Scopus says there are " + totalArticleCount + " citations, but I see " + result.size)
  //      require(false)
  //    }
  //    result
  //  }
}