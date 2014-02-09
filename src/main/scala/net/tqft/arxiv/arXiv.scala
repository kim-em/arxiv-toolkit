package net.tqft.arxiv
import java.util.Date
import java.net.URL
import net.tqft.util.Throttle

trait arXiv {
  val apiEndpoint: String = "http://export.arxiv.org/api/"
  
  def lookup(identifiers: List[String]): Map[String, Article] = {
    import com.sun.syndication.feed.synd._
    import com.sun.syndication.io.SyndFeedInput
    import com.sun.syndication.io.XmlReader

    import scala.collection.JavaConverters._
    
    val feedUrl = new URL(apiEndpoint + "query?id_list=" + identifiers.mkString(",") + "&max_results=10000")
    Throttle("arxiv.org")
    
    val feed = new SyndFeedInput().build(new XmlReader(feedUrl))
    
    (for(entry: SyndEntry <- feed.getEntries().asScala.toList.asInstanceOf[List[SyndEntry]]) yield {
    	val article = Article.fromAtomEntry(entry)
      article.identifier -> article
    }).toMap
  }
  def lookup(identifier: String): Option[Article] = lookup(List(identifier)).get(identifier)
}

trait arXivCache extends arXiv {
  private val cache: scala.collection.mutable.Map[String, (Date, Option[Article])] = scala.collection.mutable.Map()

  override def lookup(identifiers: List[String]): Map[String, Article] = {
    def fresh(date: Date) = true // FIXME decide if the date is before midnight

    val (cached, needed) = identifiers.partition(i => cache.get(i) match { case Some((date, option)) if fresh(date) => true; case _ => false })

    val now = new Date()
    val looked = super.lookup(needed)
    for ((i, a) <- looked) {
      cache += ((i, (now, Some(a))))
    }
    for (i <- needed filterNot (looked.keySet.contains _)) {
      cache += ((i, (now, None)))
    }
    (for (i <- identifiers; (_, Some(a)) <- cache.get(i)) yield (i -> a)).toMap
  }
}

object arXiv extends arXiv with arXivCache