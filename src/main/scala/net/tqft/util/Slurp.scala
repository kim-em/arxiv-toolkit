package net.tqft.util
import scala.io.Source
import java.util.Date
import net.tqft.toolkit.Logging
import java.net.URL

trait Slurp {
  def apply(url: String) = Source.fromURL(url).getLines
}

trait ThrottledSlurp extends Slurp {
  override def apply(url: String) = {
    Throttle(new URL(url).getHost.split("\\.").takeRight(2).mkString("."))
    super.apply(url)
  }
}

object Throttle extends Logging {
  val defaultInterval = 0
  val hostIntervals = Map("ams.org" -> 5000, "arxiv.org" -> 3000, "google.com" -> 500)
  val lastThrottle = scala.collection.mutable.Map[String, Long]().withDefaultValue(0)

  def apply(host: String) {
    val interval = hostIntervals.get(host).getOrElse(defaultInterval)
    def now = new Date().getTime
    while (lastThrottle(host) + interval > now) {
      info("Throttling access to " + host)
      Thread.sleep(interval)
    }
    info("Allowing access to " + host)
    lastThrottle += ((host, now))
  }
}

object Slurp extends ThrottledSlurp

