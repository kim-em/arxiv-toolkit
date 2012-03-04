package net.tqft.arxiv
import java.util.Date

trait arXiv {
	def lookup(identifier: String): Option[Article] = ???
}

trait arXivThrottler extends arXiv {
  protected def interval: Int // milliseconds
  
  private def now = new Date().getTime
  
  private var lastLookup = now - interval
  override def lookup(identifier: String): Option[Article] = {
    while(now < lastLookup + interval) {
      Thread.sleep(interval)
    }
    lastLookup = now
    super.lookup(identifier)
  }
}

trait arXivCache extends arXiv {
  private val cache: scala.collection.mutable.Map[String, (Date, Option[Article])] = scala.collection.mutable.Map()
  
  override def lookup(identifier: String): Option[Article] = {
    def fresh(date: Date) = true // FIXME decide if the date is before midnight
    
    cache.get(identifier) match {
      case Some((date, option)) if fresh(date) => option
      case _ => {
        val result = super.lookup(identifier)
        cache += ((identifier, (new Date(), result)))
        result
      }
    }
  }
}

object arXiv extends arXiv with arXivThrottler with arXivCache {
  val interval = 3000 // default 3 second delay interval, per request at http://arxiv.org/help/api/user-manual
}