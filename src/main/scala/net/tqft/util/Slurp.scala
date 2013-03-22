package net.tqft.util
import scala.io.Source
import java.util.Date
import net.tqft.toolkit.Logging
import java.net.URL
import org.apache.commons.io.IOUtils
import net.tqft.toolkit.amazon.S3

trait Slurp {
  def apply(url: String) = Source.fromURL(url).getLines
  def attempt(url: String): Either[Iterator[String], Throwable] = {
    try {
      Left(apply(url))
    } catch {
      case e: Throwable => Right(e)
    }
  }
}

trait CachingSlurp extends Slurp {
  def cache: scala.collection.mutable.Map[String, Array[Byte]]
  override def apply(url: String) = {
    Source.fromBytes(cache.getOrElseUpdate(url, {
      Throttle(new URL(url).getHost.split("\\.").takeRight(2).mkString("."))
      Logging.info("Loading " + url)
      IOUtils.toByteArray(new URL(url).openStream())
    })).getLines
  }
}

trait S3CachingSlurp extends CachingSlurp {
  def s3: S3
  def bucket: String

  override lazy val cache = s3.bytes(bucket)
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

object Slurp extends S3CachingSlurp {
  override val s3 = S3
  override val bucket = "LoM-page-cache"
}

