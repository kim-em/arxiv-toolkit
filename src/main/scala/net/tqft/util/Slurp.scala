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
import java.io.IOException
import org.apache.http.HttpException
import org.openqa.selenium.By
import java.io.BufferedInputStream
import com.ibm.icu.text.CharsetDetector
import java.nio.charset.UnsupportedCharsetException
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import com.gargoylesoftware.htmlunit.BrowserVersion
import org.apache.http.impl.conn.PoolingClientConnectionManager
import org.apache.http.impl.conn.SchemeRegistryFactory
import org.apache.http.params.HttpConnectionParams
import org.apache.http.impl.conn.BasicClientConnectionManager
import java.io.FilterInputStream

trait Slurp {
  def getStream(url: String): InputStream = new URL(url).openStream
  final def getBytes(url: String) = IOUtils.toByteArray(getStream(url))
  
  final def apply(url: String) = {
    val bis = new BufferedInputStream(getStream(url));
    val cd = new CharsetDetector();
    cd.setText(bis);
    val cm = cd.detect();

    if (cm != null) {
      val reader = cm.getReader();
      val charset = cm.getName();
      //      Logging.info("reading stream in charset " + charset)
      
      class ClosingIterator[A](i: Iterator[A]) extends Iterator[A] {
        var open = true
        
        override def hasNext = {
          open && i.hasNext match {
            case true => true
            case false => {
              open = false
              bis.close
              false
            }
          }
        }
        override def next = i.next
      }
      
      new ClosingIterator(Source.fromInputStream(bis, charset).getLines): Iterator[String]
    } else {
      throw new UnsupportedCharsetException("")
    }
  }
  final def attempt(url: String): Either[Iterator[String], Throwable] = {
    try {
      Left(apply(url))
    } catch {
      case e @ (_: IOException | _: HttpException) => {
        Right(e)
      }
    }
  }

  def getString(url: String) = apply(url).mkString("\n")
}

trait HttpClientSlurp extends Slurp {
  val cxMgr = new PoolingClientConnectionManager(SchemeRegistryFactory.createDefault());
  cxMgr.setMaxTotal(100);
  cxMgr.setDefaultMaxPerRoute(20);

  val client: HttpClient = new DecompressingHttpClient(new DefaultHttpClient(cxMgr))
  
  val params = client.getParams()
  params.setBooleanParameter("http.protocol.handle-redirects", true)
  HttpConnectionParams.setConnectionTimeout(params, 10000);
  HttpConnectionParams.setSoTimeout(params, 10000);

  def useragent = "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)"
  HttpProtocolParams.setUserAgent(params, useragent);

  override def getStream(url: String) = getStream(url, None)
  def getStream(url: String, referer: Option[String]) = {
    val get = new HttpGet(url)
    get.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    referer.map(r => get.setHeader("Referer", r))

    val response = client.execute(get);
    
    class ReleasingInputStream(is: InputStream) extends FilterInputStream(is) {
      override def close() {
        get.releaseConnection()
        is.close()
      }
    }
    
    new ReleasingInputStream(response.getEntity.getContent())
  }
}

object HttpClientSlurp extends HttpClientSlurp

trait FirefoxSlurp extends Slurp {
  private def driver = FirefoxSlurp.driverInstance

  override def getStream(url: String) = {
    import scala.collection.JavaConverters._

    if (FirefoxSlurp.enabled_?) {
      try {
        driver.findElements(By.cssSelector("""a[href="""" + url + """"]""")).asScala.headOption match {
          case Some(element) => {
            Logging.info("webdriver: clicking an available link")
            element.click()
          }
          case None => driver.get(url)
        }

        // TODO more validation we really arrived?
        driver.getTitle match {
          case e @ ("502 Bad Gateway" | "500 Internal Server Error" | "503 Service Temporarily Unavailable") => throw new HttpException(e)
          case e @ ("MathSciNet Access Error") => throw new HttpException("403 " + e)
          case _ =>
        }
        new ByteArrayInputStream(driver.getPageSource.getBytes("UTF-8"))
      } catch {
        case e @ (_: org.openqa.selenium.NoSuchWindowException | _: org.openqa.selenium.remote.UnreachableBrowserException) => {
          Logging.warn("Browser window closed, trying to restart Firefox/webdriver")
          FirefoxSlurp.quit
          Logging.info("retrying ...")
          getStream(url)
        }
      }
    } else {
      throw new IllegalStateException("slurping via Selenium has been disabled, but someone asked for a URL: " + url)
    }
  }
}

trait HtmlUnitSlurp extends Slurp {
  private def driver = HtmlUnitSlurp.driverInstance

  override def getStream(url: String) = {
    import scala.collection.JavaConverters._

    if (HtmlUnitSlurp.enabled_?) {
      try {
        driver.findElements(By.cssSelector("""a[href="""" + url + """"]""")).asScala.headOption match {
          case Some(element) => {
            Logging.info("webdriver: clicking an available link")
            element.click()
          }
          case None => driver.get(url)
        }

        // TODO more validation we really arrived?
        driver.getTitle match {
          case e @ ("502 Bad Gateway" | "500 Internal Server Error") => throw new HttpException(e)
          case e @ ("MathSciNet Access Error") => throw new HttpException("403 " + e)
          case _ =>
        }
        new ByteArrayInputStream(driver.getPageSource.getBytes("UTF-8"))
      } catch {
        case e: Exception => {
          e.printStackTrace()
          Logging.warn("Browser window closed, trying to restart HtmlUnit")
          HtmlUnitSlurp.quit
          Logging.info("retrying ...")
          getStream(url)
        }
      }
    } else {
      throw new IllegalStateException("slurping via HtmlUnit has been disabled, but someone asked for a URL: " + url)
    }
  }

}

object HtmlUnitSlurp extends HtmlUnitSlurp {
  private var driverOption: Option[WebDriver] = None

  def driverInstance = {
    if (driverOption.isEmpty) {
      Logging.info("Starting HtmlUnit/webdriver")
      driverOption = Some(new HtmlUnitDriver(BrowserVersion.FIREFOX_17))
      Logging.info("   ... finished starting HTMLUnit")
    }
    driverOption.get
  }

  def quit = {
    driverOption.map(_.quit)
    driverOption = None
  }

  private var enabled = true
//  def disable = enabled = false
  def enabled_? = enabled
}

object FirefoxSlurp extends FirefoxSlurp {
  private var driverOption: Option[WebDriver] = None

  def driverInstance = {
    if (driverOption.isEmpty) {
      Logging.info("Starting Firefox/webdriver")
      driverOption = Some(new FirefoxDriver())
      Logging.info("   ... finished starting Firefox")
    }
    driverOption.get
  }

  def quit = {
    driverOption.map(_.quit)
    driverOption = None
  }

  private var enabled = true
  def disable = {
    Logging.warn("Disabling FirefoxSlurp")
    enabled = false
  }
  def enabled_? = enabled
}

trait MathSciNetMirrorSlurp extends Slurp {
  val offset = Random.nextInt(10 * 60 * 1000)
  val mirrorList = Random.shuffle(List( /*"www.ams.org", */ "ams.rice.edu", "ams.impa.br", "ams.math.uni-bielefeld.de", "ams.mpim-bonn.mpg.de", "ams.u-strasbg.fr"))
  def mirror = mirrorList((((new Date().getTime() + offset) / (10 * 60 * 1000)) % mirrorList.size).toInt)

  override def getStream(url: String) = {
    val newURL = if (url.startsWith("http://www.ams.org/mathscinet/")) {
      "http://" + mirror + "/mathscinet/" + url.stripPrefix("http://www.ams.org/mathscinet/")
    } else {
      url
    }
    super.getStream(newURL)
  }
}

trait CachingSlurp extends Slurp {
  protected def cache(hostName: String): scala.collection.mutable.Map[String, Array[Byte]]

  override def getStream(url: String) = {
    Logging.info("Looking in cache for " + url)
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
  val hostIntervals = scala.collection.mutable.Map("ams.org" -> 150000, "arxiv.org" -> 5000, "google.com" -> 500)
  val lastThrottle = scala.collection.mutable.Map[String, Long]().withDefaultValue(0)

  // poisson distributed gaps
  private def exponentialDistribution(mean: Int) = {
    (-mean * (Math.log(1.0 - scala.util.Random.nextDouble())))
  }
  private def normalDistribution = {
    import scala.math._
    import scala.util.Random.nextDouble
    sqrt(-2 * log(nextDouble)) * cos(2 * Pi * nextDouble)
  }
  private def logNormalDistribution(mean: Double, shape: Double = 1) = {
    import scala.math._
    val sigma = sqrt(shape)
    val mu = log(mean) - shape / 2
    exp(mu + sigma * normalDistribution)
  }

  def apply(url: String) {
    val domain = new URL(url).getHost.split("\\.").takeRight(2).mkString(".")

    val interval = hostIntervals.get(domain).getOrElse(defaultInterval)
    def now = new Date().getTime
    if (lastThrottle(domain) + interval > now) {
      val delay = logNormalDistribution(interval).toLong
      info("Throttling access to " + domain + " for " + delay / 1000.0 + " seconds")
      Thread.sleep(delay)
    }
    info("Allowing access to " + domain)
    lastThrottle += ((domain, now))
  }
}

object Slurp extends FirefoxSlurp with MathSciNetMirrorSlurp with S3CachingSlurp {
  override val s3 = AnonymousS3
  override val bucketSuffix = ".cache"
}

