package net.tqft.mathscinet
import net.tqft.util.Slurp
import net.tqft.util.URLEncode
import net.tqft.arxiv.arXiv
import net.tqft.util.BIBTEX
import java.io.File
import scala.io.Source
import net.tqft.toolkit.amazon.AnonymousS3
import net.tqft.toolkit.Extractors.Int
import java.net.URL
import net.tqft.toolkit.Logging
import java.io.BufferedInputStream
import org.apache.commons.io.IOUtils
import eu.medsea.mimeutil.MimeUtil
import net.tqft.util.Http
import net.tqft.util.HttpClientSlurp
import org.apache.commons.io.FileUtils
import java.io.InputStream
import net.tqft.util.Html
import scala.collection.parallel.ForkJoinTaskSupport

trait Article {
  def identifier: Int
  def identifierString = "MR" + ("0000000" + identifier.toString).takeRight(7)
  def shortIdentifierString = "MR" + identifier.toString

  def MathSciNetURL = "http://www.ams.org/mathscinet-getitem?mr=" + identifier
  def bibtexURL = "http://www.ams.org/mathscinet/search/publications.html?fmt=bibtex&pg1=MR&s1=" + identifier
  def endnoteURL = "http://www.ams.org/mathscinet/search/publications.html?fmt=endnote&pg1=MR&s1=" + identifier

  lazy val slurp = Slurp(MathSciNetURL).toList

  var endnoteData: Option[Map[String, List[String]]] = None
  var bibtexData: Option[BIBTEX] = None

  def endnote = {
    if (endnoteData.isEmpty) {
      val lines = Slurp(endnoteURL).toList
      val start = lines.indexWhere(_.trim == "<pre>")
      val finish = lines.indexWhere(_.trim == "</pre>")
      endnoteData = Some(lines.slice(start + 1, finish).groupBy(_.take(2)).mapValues(l => l.map(_.drop(3))))
    }
    endnoteData.get
  }
  def bibtex = {
    if (bibtexData.isEmpty) {
      val text = BIBTEX.cache.get(shortIdentifierString).getOrElse(BIBTEX.cache.getOrElseUpdate(identifierString, {
        val lines = Slurp(bibtexURL).toList
        val start = lines.indexWhere(_.trim == "<pre>")
        val finish = lines.indexWhere(_.trim == "</pre>")
        lines.slice(start + 1, finish).mkString("\n")
      }))
      bibtexData = BIBTEX.parse(text)
    }
    bibtexData.get
  }

  def title: String = {
    if (endnoteData.nonEmpty) {
      endnote("%T").head
    } else {
      bibtex.get("TITLE").get
    }
  }
  // FIXME load from endnote or bibtex
  def authors: List[Author] = endnote("%A").map(Author(_))
  def journalReference: String = endnote("%J").head + " " + endnote("%V").head + " (" + endnote("%D").head + "), no. " + endnote("%N").head + ", " + endnote("%P").head

  def year: Int = {
    bibtex.get("YEAR").get.toInt
  }
  def volume: Int = {
    bibtex.get("VOLUME").get.toInt
  }
  def number: Int = {
    bibtex.get("NUMBER").get.toInt
  }

  def pageStart: Option[Int] = {
    val pages = bibtex.get("PAGES")
    pages flatMap { p =>
      val pageString = if (p.contains("--")) {
        p.split("--")(0)
      } else {
        p
      }
      pageString match {
        case Int(i) => Some(i)
        case _ => None
      }
    }
  }
  def pageEnd: Option[Int] = {
    val pages = bibtex.get("PAGES")
    pages flatMap { p =>
      val pageString = if (p.contains("--")) {
        Some(p.split("--")(1))
      } else {
        None
      }
      pageString flatMap {
        case Int(i) => Some(i)
        case _ => None
      }
    }
  }

  def DOI: Option[String] = {
    // TODO There are probably other replacements needed; do this properly?
    // &gt; and &lt; appear in Wiley DOIs.
    bibtex.get("DOI").map(_.replaceAllLiterally("&gt;", ">").replaceAllLiterally("&lt;", "<").replaceAllLiterally("&amp;", "&"))
  }

  def URL: Option[String] = {
    bibtex.get("URL")
  }

  lazy val pdfURL: Option[String] = {
    // This mimics the logic of direct-article-link.user.js 

    URL flatMap { url =>
      url match {
        // Elsevier
        // 10.1006 10.1016, Elsevier, has complicated URLs, e.g.
        // 10.1006/jabr.1996.0306 ---resolves to---> http://www.sciencedirect.com/science/article/pii/S0021869396903063
        //						  ---follow link---> http://pdn.sciencedirect.com/science?_ob=MiamiImageURL&_cid=272332&_user=10&_pii=S0021869396903063&_check=y&_origin=article&_zone=toolbar&_coverDate=1996--15&view=c&originContentFamily=serial&wchp=dGLbVlt-zSkWz&md5=fb951ad4ff13953e97dc2afd6fd16d4a&pid=1-s2.0-S0021869396903063-main.pdf
        //                        ---resolves to---> http://ac.els-cdn.com/S0021869396903063/1-s2.0-S0021869396903063-main.pdf?_tid=756d984e-a048-11e2-8b82-00000aab0f02&acdnat=1365424565_666b1bf7394bbc91c15fac27d45952a0
        // 10.1016/0167-6687(83)90020-3 ---resolves to---> http://www.sciencedirect.com/science/article/pii/0167668783900203#
        //                              ---follow link (from campus)---> http://pdn.sciencedirect.com/science?_ob=MiamiImageURL&_cid=271685&_user=554534&_pii=0167668783900203&_check=y&_origin=article&_zone=toolbar&_coverDate=30-Apr-1983&view=c&originContentFamily=serial&wchp=dGLbVlt-zSkzS&md5=729643f534e9cc7abe5882e10cca9e40&pid=1-s2.0-0167668783900203-main.pdf
        // 								---follow link (off campus)----> http://pdn.sciencedirect.com/science?_ob=ShoppingCartURL&_method=add&_eid=1-s2.0-0167668783900203&originContentFamily=serial&_origin=article&_acct=C000228598&_version=1&_userid=10&_ts=1365482216&md5=ecfe2869e3c92d58e7f05c5762d02d90
        case url if url.startsWith("http://dx.doi.org/10.1006") || url.startsWith("http://dx.doi.org/10.1016") => {
          // If we're not logged in, it's not going to work. Just use the HttpClientSlurp, and don't make any attempt to save the answer.
          val regex = """pdfurl="([^"]*)"""".r
          regex.findFirstMatchIn(HttpClientSlurp(url).mkString("\n")).map(m => m.group(1))
        }
        // TODO imitate this in direct-article-link
        // over there using jQuery is a bit problematic; maybe  it's worth finding the regex that does this!
        // Cambridge University Press
        // 10.1017 10.1051
        // 10.1017/S0022112010001734 ---resolves to---> http://journals.cambridge.org/action/displayAbstract?fromPage=online&aid=7829674
        //						   ---follow "View PDF (" (or jQuery for "a.article-pdf")---> http://journals.cambridge.org/action/displayFulltext?type=1&fid=7829676&jid=FLM&volumeId=655&issueId=-1&aid=7829674&bodyId=&membershipNumber=&societyETOCSession=
        //						   ---resolves to something like---> http://journals.cambridge.org/download.php?file=%2FFLM%2FFLM655%2FS0022112010001734a.pdf&code=ac265aacb742b93fa69d566e33aeaf5e
        case url if url.startsWith("http://dx.doi.org/10.1017/S") || url.startsWith("http://dx.doi.org/10.1051/S") || url.startsWith("http://dx.doi.org/10.1112/S0010437X") => {
          val scrape = Html.jQuery(url).get("a.article-pdf").first
          if (scrape.size == 1) {
            Some("http://journals.cambridge.org/action/" + scrape.attribute("href").replaceAll("\n", "").replaceAll("\t", "").replaceAll(" ", ""))
          } else {
            Logging.warn("Looking for PDF link on CUP page " + url + " failed, found " + scrape.size + " results")
            None
          }
        }

        // TODO imitate this in direct-article-link
        // Wiley
        // 10.1002/(SICI)1097-0312(199602)49:2<85::AID-CPA1>3.0.CO;2-2 ---resolves to---> http://onlinelibrary.wiley.com/doi/10.1002/(SICI)1097-0312(199602)49:2%3C85::AID-CPA1%3E3.0.CO;2-2/abstract
        // 															 ---links to--->    http://onlinelibrary.wiley.com/doi/10.1002/(SICI)1097-0312(199602)49:2%3C85::AID-CPA1%3E3.0.CO;2-2/pdf
        //															 ---???---> http://onlinelibrary.wiley.com/store/10.1002/(SICI)1097-0312(199602)49:2%3C85::AID-CPA1%3E3.0.CO;2-2/asset/1_ftp.pdf?v=1&t=hfc3fjoo&s=dc6fad69f11cfc2ff2f302f5d1386c553d48f47c
        // the mystery step here is somewhat strange; it looks like it contains an iframe, but then redirects to the contents of the iframe?
        case url if url.startsWith("http://dx.doi.org/10.1002/") => {
          val regex = """id="pdfDocument" src="([^"]*)"""".r
          val url2 = "http://onlinelibrary.wiley.com/doi/" + url.stripPrefix("http://dx.doi.org/") + "/pdf"
          val slurped = HttpClientSlurp(url2).mkString("\n")
          regex.findFirstMatchIn(slurped).map(m => m.group(1))
        }

        // otherwise, try using DOI-direct
        case url if url.startsWith("http://dx.doi.org/") => {
          Http.findRedirect(url.replaceAllLiterally("http://dx.doi.org/", "http://evening-headland-2959.herokuapp.com/")) match {
            case None => None
            case Some(redirect) if redirect.startsWith("http://dx.doi.org/") => {
              // didn't learn anything
              None
            }
            case Some(redirect) => Some(redirect)
          }
        }
        case url if url.startsWith("http://projecteuclid.org/getRecord?id=") => {
          Some(url.replaceAllLiterally("http://projecteuclid.org/getRecord?id=", "http://projecteuclid.org/DPubS/Repository/1.0/Disseminate?view=body&id=pdf_1&handle="))
        }
        case url if url.startsWith("http://www.numdam.org/item?id=") => {
          Some(url.replaceAllLiterally("http://www.numdam.org/item?id=", "http://archive.numdam.org/article/") + ".pdf")
        }
      }
    }
  }

  def pdfInputStream: Option[InputStream] = {
    DOI flatMap { doi =>
      doi match {
        case doi if doi.startsWith("10.1215") => {
          // Duke expects to see a Referer field.
          pdfURL.map({ u =>
            HttpClientSlurp.getStream(u, referer = Some(u))
          })
        }
        case _ => {
          pdfURL.map(HttpClientSlurp.getStream)
        }
      }
    }
  }

  def pdf: Option[Array[Byte]] = {
    pdfInputStream.flatMap(stream => {
      val bis = new BufferedInputStream(stream)
      bis.mark(20)
      val prefix = new Array[Byte](10)
      bis.read(prefix)
      bis.reset
      val prefixString = new String(prefix)
      val mimetype = if (prefixString.contains("%PDF")) {
        "application/pdf"
      } else {
        MimeUtil.getMimeTypes(bis).toString
      }
      mimetype match {
        case "application/pdf" => {
          Logging.info("Obtained bytes for PDF for " + identifierString)
          Some(IOUtils.toByteArray(bis))
        }
        case t => {
          Logging.warn("Content does not appear to be a PDF! (File begins with " + prefixString + " and MIME type detected as " + t + ".)")
          Logging.warn(IOUtils.toString(bis))
          bis.close
          None
        }
      }
    })
  }

  def savePDF(directory: File) {
    val file = new File(directory, identifierString + ".pdf")
    if (file.exists()) {
      Logging.info("PDF for " + identifierString + " already exists in " + directory)
    } else {
      for (bytes <- pdf) {
        FileUtils.writeByteArrayToFile(file, bytes)
      }
    }
  }
}

object Article {
  def apply(identifierString: String): Article = {
    apply(identifierString.stripPrefix("MR").toInt)
  }

  def apply(identifier: Int): Article = {
    val identifier_ = identifier
    new Article {
      override val identifier = identifier_
    }
  }

  def fromDOI(doi: String): Option[Article] = {
    val result = AnonymousS3("DOI2mathscinet").get(doi).map({ identifierString =>
      val identifier_ = identifierString.stripPrefix("MR").toInt
      new Article {
        override val identifier = identifier_
        override val DOI = Some(doi)
      }
    })
    if (result.isEmpty) {
      Logging.info("No mathscinet entry found for DOI: " + doi)
    }
    result
  }

  def fromBibtex(bibtexString: String): Option[Article] = {
    BIBTEX.parse(bibtexString).map({
      case b @ BIBTEX(_, identifierString @ MRIdentifier(id), data) =>
        if (saving_?) b.save
        val result = new Article {
          override val identifier = id
        }
        result.bibtexData = Some(b)
        result
    })
  }

  private var saving_? = true
  def disableBibtexSaving { saving_? = false }
}

object MRIdentifier {
  def unapply(s: String): Option[Int] = {
    import net.tqft.toolkit.Extractors.Int
    s.stripPrefix("MR") match {
      case Int(id) => Some(id)
      case _ => None
    }
  }
}

object Articles {
  def withCachedBIBTEX: Iterator[Article] = {
    BIBTEX.cache.keysIterator.collect({ case MRIdentifier(id) => Article(id) })
  }

  def fromBibtexFile(file: String): Iterator[Article] = {
    import net.tqft.toolkit.collections.Split._
    Source.fromFile(file).getLines.splitOn(_.isEmpty).map(_.mkString("\n")).grouped(100).flatMap(_.par.flatMap(Article.fromBibtex))
  }

  def withDOIPrefix(prefix: String): Iterator[Article] = {
    val pool = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(50))
    for (group <- AnonymousS3("DOI2mathscinet").keysWithPrefix(prefix).iterator.grouped(1000); doi <- { val p = group.par; p.tasksupport = pool; p }; article <- Article.fromDOI(doi)) yield article
  }

}