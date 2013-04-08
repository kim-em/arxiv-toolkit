package net.tqft.arxiv
import java.net.URL
import net.tqft.util.Throttle

trait Author {
  def name: String

  def articles: List[Article] = {
    val apiEndpoint: String = "http://export.arxiv.org/api/" // FIXME this shouldn't be reduplicated

    import com.sun.syndication.feed.synd._
    import com.sun.syndication.io.SyndFeedInput
    import com.sun.syndication.io.XmlReader

    import scala.collection.JavaConverters._

    val feedUrl = apiEndpoint + "query?search_query=" + name + "&max_results=10000"
    Throttle(feedUrl)

    val feed = new SyndFeedInput().build(new XmlReader(new URL(feedUrl)))
    for(entry: SyndEntry <- feed.getEntries().asScala.toList.asInstanceOf[List[SyndEntry]]) yield {
      Article.fromAtomEntry(entry, true)
    }
  }
}

object Author {
  def lookup(name: String): Option[Author] = {
    val name_ = name
    Some(new Author { val name = name_ })
  }
}