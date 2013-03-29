package net.tqft.util
import scala.io.Source
import java.util.Date
import net.tqft.toolkit.Logging
import java.net.URL
import org.apache.commons.io.IOUtils
import net.tqft.toolkit.amazon.S3
import net.tqft.toolkit.amazon.AnonymousS3
import java.io.ByteArrayInputStream
import java.io.InputStream
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.params.HttpProtocolParams
import org.apache.http.impl.client.DecompressingHttpClient
import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver
import scala.util.Random

trait Slurp {
  def getStream(url: String): InputStream = new URL(url).openStream
  final def getBytes(url: String) = IOUtils.toByteArray(getStream(url))

  final def apply(url: String) = Source.fromInputStream(getStream(url)).getLines
  final def attempt(url: String): Either[Iterator[String], Throwable] = {
    try {
      Left(apply(url))
    } catch {
      case e: Throwable => {
        Right(e)
      }
    }
  }
}

trait HttpClientSlurp extends Slurp {
  val client: HttpClient = new DecompressingHttpClient(new DefaultHttpClient)
  client.getParams().setBooleanParameter("http.protocol.handle-redirects", true)

  def useragent = "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)"
  HttpProtocolParams.setUserAgent(client.getParams(), useragent);

  override def getStream(url: String) = {
    val get = new HttpGet(url)
    get.setHeader("Accept", "text/html,application/xhtml+xml,application/xml");

    val response = client.execute(get);
    response.getEntity.getContent()
  }
}

trait SeleniumSlurp extends Slurp {
  lazy val driver: WebDriver = {
    Logging.info("Starting Firefox/webdriver")
    val result = new FirefoxDriver()
    Logging.info("   ... finished starting Firefox")
    result
  }

  override def getStream(url: String) = {
    // TODO see if the url is available as a link, and if so follow it?
    driver.get(url)
    // TODO some validation we really arrived?
    new ByteArrayInputStream(driver.getPageSource.getBytes())
  }

}

trait MathSciNetMirrorSlurp extends Slurp {
  val offset = Random.nextInt(10 * 60 * 1000)
  val mirrorList = Random.shuffle(List("www.ams.org", "ams.rice.edu", "ams.impa.br", "ams.math.uni-bielefeld.de", "ams.mpim-bonn.mpg.de", "ams.u-strasbg.fr"))
  def mirror = mirrorList((((new Date().getTime() + offset) / (10 * 60 * 1000)) % mirrorList.size).toInt)

  override def getStream(url: String) = {
    val newURL = if (url.startsWith("http://www.ams.org/mathscinet")) {
      "http://" + mirror + "/mathscinet" + url.stripPrefix("http://www.ams.org/mathscinet")
    } else {
      url
    }
    super.getStream(newURL)
  }
}

trait CachingSlurp extends Slurp {
  protected def cache(hostName: String): scala.collection.mutable.Map[String, Array[Byte]]

  override def getStream(url: String) = {
    val bytes = cache(url).getOrElseUpdate(url, {
      Throttle(url)
      Logging.info("Loading " + url)
      val result = IOUtils.toByteArray(super.getStream(url))
      Logging.info("   ... finished")
      result
    })
    new ByteArrayInputStream(bytes)
  }
}

trait S3CachingSlurp extends CachingSlurp {
  def s3: S3
  def bucketSuffix: String

  private val caches = {
    import net.tqft.toolkit.functions.Memo
    Memo({ hostName: String =>
      {
        import net.tqft.toolkit.collections.MapTransformer._
        s3.bytes(hostName + bucketSuffix).transformKeys({ relativeURL: String => "http://" + hostName + "/" + relativeURL }, { absoluteURL: String => new URL(absoluteURL).getFile().stripPrefix("/") })
      }
    })
  }

  override def cache(url: String) = {
    val hostName = new URL(url).getHost
    caches(hostName)
  }
}

trait ThrottledSlurp extends Slurp {
  override def getStream(url: String) = {
    Throttle(url)
    super.getStream(url)
  }
}

object Throttle extends Logging {
  val defaultInterval = 1000
  val hostIntervals = scala.collection.mutable.Map("ams.org" -> 10000, "arxiv.org" -> 5000, "google.com" -> 500)
  val lastThrottle = scala.collection.mutable.Map[String, Long]().withDefaultValue(0)

  def apply(host: String) {
    val domain = new URL(host).getHost.split("\\.").takeRight(2).mkString(".")

    val interval = hostIntervals.get(domain).getOrElse(defaultInterval)
    def now = new Date().getTime
    while (lastThrottle(domain) + interval > now) {
      info("Throttling access to " + host)
      Thread.sleep(interval)
    }
    info("Allowing access to " + host)
    lastThrottle += ((domain, now))
  }
}

object Slurp extends SeleniumSlurp with MathSciNetMirrorSlurp with S3CachingSlurp {
  override val s3 = AnonymousS3
  override val bucketSuffix = ".cache"
}

