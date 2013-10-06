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
import net.tqft.util.Accents
import java.io.FilenameFilter
import org.apache.commons.lang3.StringUtils
import net.tqft.journals.ISSNs
import java.util.zip.GZIPInputStream
import java.io.FileInputStream
import net.tqft.util.pandoc

trait Article {
  def identifier: Int
  def identifierString = "MR" + ("0000000" + identifier.toString).takeRight(7)
  //  def shortIdentifierString = "MR" + identifier.toString

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
      val text = BIBTEX.cache.getOrElseUpdate(identifierString, {
        val lines = Slurp(bibtexURL).toList
        val start = lines.indexWhere(_.trim.startsWith("<pre>"))
        val finish = lines.indexWhere(_.trim == "</pre>")
        if (start == -1 || finish <= start) {
          Logging.warn("Did not find BIBTEX block in: \n" + lines.mkString("\n"))
          ???
        }
        (lines(start).trim.stripPrefix("<pre>") +: lines.slice(start + 1, finish)).mkString("\n").trim
      })
      try {
        bibtexData = BIBTEX.parse(text)
      } catch {
        case e: Exception => {
          Logging.error("Exception while parsing BIBTEX for " + identifierString + ": \n" + text, e)
          try {
            BIBTEX.cache -= identifierString
          } catch {
            case e: Exception => Logging.warn("Failed to clean BIBTEX database.")
          }
          ???
        }
      }
    }
    bibtexData.get
  }

  def title: String = {
    if (endnoteData.nonEmpty) {
      endnote("%T").head
    } else {
      bibtex.get("TITLE").getOrElse("Untitled")
    }
  }

  def authors: List[Author] = {
    if (endnoteData.nonEmpty) {
      endnote("%A").map(n => Author(0, n))
    } else {
      bibtex.get("AUTHOR") match {
        case None => List()
        case Some(a) => a.split(" and ").toList.map(a => Author(0, a /*Accents.LaTeXToUnicode(a)*/ ))
      }
    }
  }

  def journalOption = bibtex.get("JOURNAL")
  def journal = journalOption.get

  def citation: String = {
    def restOfCitation = volumeOption.map(" " + _).getOrElse("") + yearStringOption.map(" (" + _ + "), ").getOrElse("") + numberOption.map("no. " + _ + ", ").getOrElse("") + pagesOption.getOrElse("")

    bibtex.documentType match {
      case "article" => {
        journal + restOfCitation
      }
      case "book" => {
        bibtex.get("ISBN").map("ISBN: " + _).getOrElse("")
      }
      case "inproceedings" => {
        bibtex.get("BOOKTITLE").map(" " + _).getOrElse("") + journalOption.getOrElse("") + restOfCitation
      }
      case "proceedings" => {
        bibtex.get("NOTE").getOrElse("")
      }
      case "incollection" => {
        bibtex.get("BOOKTITLE").map(_ + " ").getOrElse("") + pages
      }
      case otherwise => {
        Logging.warn("Citation format for " + identifierString + " of type " + otherwise + " undefined:\n" + bibtex.toBIBTEXString)
        ???
      }
    }
  }

  def yearStringOption: Option[String] = {
    bibtex.get("YEAR").map(y => y.replace("/", "-"))
  }
  def yearOption: Option[Int] = {
    bibtex.get("YEAR").flatMap({
      case Int(year) => Some(year)
      case _ => None
    })
  }
  def year = yearOption.get
  def volumeOption: Option[Int] = {
    bibtex.get("VOLUME").flatMap({
      case Int(v) => Some(v)
      case _ => None
    })
  }
  def volume: Int = volumeOption.get

  // Numbers are not always integers, e.g. "fasc. 1" in Advances
  def numberOption = bibtex.get("NUMBER")
  def number: String = numberOption.get

  def ISSNOption = bibtex.get("ISSN")
  def ISSN = ISSNOption.get

  def pagesOption = bibtex.get("PAGES")
  def pages = pagesOption.get

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

  def numberOfCitations: Int = {
    val regex = "From References: ([0-9]*)".r 
    slurp.flatMap(line => regex.findFirstMatchIn(line).map(_.group(1).toInt)).headOption.getOrElse(0)
  }
  def citations: Iterator[Article] = Search.citing(identifier)
  
  lazy val pdfURL: Option[String] = {
    // This mimics the logic of direct-article-link.user.js 

    if (ISSNs.Elsevier.contains(ISSN) && URL.isEmpty) {
      // Old Elsevier articles, that MathSciNet doesn't know about

      Logging.info("Attempting to find URL for Elsevier article.")
      
      val numbers = {
        // Topology, special cases
        if (ISSN == "0040-9383" && volume == 2) {
          Stream("1-2", "3", "4")
        } else if (ISSN == "0040-9383" && volume == 3) {
          Stream("supp/S1", "supp/S2", "1", "2", "3", "4")
        } else if (ISSN == ISSNs.`Advances in Mathematics` && volume == 24 && number == "2") {
          Stream("1", "2")
        } else {
          numberOption match {
            case Some(number) => Stream(number.split(" ").last)
            case _ => Stream.from(1).map(_.toString)
          }
        }
      }
      val pages = numbers.map({ n =>
        if (n == "10") {
          println("Something went wrong while looking up " + identifierString + " on the Elsevier website.")
          return None
        }
        val url = "http://www.sciencedirect.com/science/journal/" + ISSN.replaceAllLiterally("-", "") + "/" + volume + "/" + n
//        println("Scanning page: " + url)
        (url, Article.ElsevierSlurpCache(url))
      }).takeWhile({ p =>
        val lines = p._2
//        println(lines.mkString("\n"))
        val titleLine = lines.find(_.contains("<title>")).get
        println(titleLine)
        titleLine.contains("| Vol " + volume) &&
          !titleLine.contains("In Progress") &&
          !titleLine.contains("Topology | Vol 48, Isss 2–4, Pgs 41-224, (June–December, 2009)")
      }).toSeq
      
      val regex1 = """<span style="font-weight : bold ;">(.*)</span></a></h3>""".r
      val regex2 = """<a href="(http://www.sciencedirect.com/science\?_ob=MiamiImageURL.*.pdf) " target="newPdfWin"""".r
      val regex3 = """<a class="cLink" rel="nofollow" href="(http://www.sciencedirect.com/science/article/pii/.*.pdf)" queryStr="\?_origin=browseVolIssue&_zone=rslt_list_item" target="newPdfWin">""".r

      val matches = (for ((_, p) <- pages; l <- p; if l.contains("newPdfWin"); titleFound <- regex1.findFirstMatchIn(l); urlFound <- regex2.findFirstMatchIn(l).orElse(regex3.findFirstMatchIn(l))) yield {
        (titleFound.group(1), urlFound.group(1), StringUtils.getLevenshteinDistance(titleFound.group(1).replaceAll("<[^>]*>", ""), title).toDouble / title.length())
      }).sortBy(_._3)
      
      Logging.info("   found matches:")
      for(m <- matches) Logging.info(m)
      
      val chosenMatch = if (matches.filter(_._3 == 0.0).size == 1
        || matches.filter(_._3 <= 0.425).size == 1
        || (matches.filter(_._3 <= 0.425).size > 1 && matches(0)._3 < matches(1)._3 / 2)) {
        Some(matches.head._2)
      } else if (title.startsWith("Erratum") && matches.count(_._1.startsWith("Erratum")) == 1) {
        matches.find(_._1.startsWith("Erratum")).map(_._2)
      } else if (title.startsWith("Errata") && matches.count(_._1.startsWith("Errata")) == 1) {
        matches.find(_._1.startsWith("Errata")).map(_._2)
      } else if (title.startsWith("Correction to") && matches.count(_._1.startsWith("Correction to")) == 1) {
        matches.find(_._1.startsWith("Correction to")).map(_._2)
      } else if (title.startsWith("Addendum") && matches.count(_._1.startsWith("Addendum")) == 1) {
        matches.find(_._1.startsWith("Addendum")).map(_._2)
      } else if (title.startsWith("Obituary") && matches.count(_._1.startsWith("Obituary")) == 1) {
        matches.find(_._1.startsWith("Obituary")).map(_._2)
      } else if (title.startsWith("Corrigendum") && matches.count(_._1.contains("orrigendum")) == 1) {
        matches.find(_._1.contains("orrigendum")).map(_._2)
      } else {
        None
      }

      //      require(chosenMatch.nonEmpty, "\n" + title + matches.map(t => t._1 -> t._3).mkString("\n", "\n", ""))
      for (c <- chosenMatch) println("Found URL for old Elsevier article: " + c)

      chosenMatch
    } else if (ISSN == ISSNs.`K-Theory`) {
      // K-Theory, have to get it from Portico for now
      val toc = HttpClientSlurp.getString("http://www.portico.org/Portico/browse/access/toc.por?journalId=ISSN_09203036&issueId=ISSN_09203036v" + volume.toString + "i" + number)
      //      println(toc)
      val pagesPosition = toc.indexOf(pages.replaceAllLiterally("--", "-"))
      val idPosition = toc.drop(pagesPosition).indexOf("articleId=")
      val identifier = toc.drop(pagesPosition).drop(idPosition).drop("articleId=".length()).take(11)

      println(identifier)
      val result = "http://www.portico.org/Portico/article/access/DownloadPDF.por?journalId=ISSN_09203036&issueId=ISSN_09203036v" + volume.toString + "i" + number + "&articleId=" + identifier + "&fileType=pdf&fileValid=true"
      println(result)
      Some(result)
    } else {

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
          // Cambridge University Press
          // 10.1017 10.1051
          // 10.1017/S0022112010001734 ---resolves to---> http://journals.cambridge.org/action/displayAbstract?fromPage=online&aid=7829674
          //						   ---follow "View PDF (" (or jQuery for "a.article-pdf")---> http://journals.cambridge.org/action/displayFulltext?type=1&fid=7829676&jid=FLM&volumeId=655&issueId=-1&aid=7829674&bodyId=&membershipNumber=&societyETOCSession=
          //						   ---resolves to something like---> http://journals.cambridge.org/download.php?file=%2FFLM%2FFLM655%2FS0022112010001734a.pdf&code=ac265aacb742b93fa69d566e33aeaf5e
          // We also need to grab some 10.1112 DOIs, for LMS journals run by CMP e.g. 10.1112/S0010437X04001034
          case url if url.startsWith("http://dx.doi.org/10.1017/S") || url.startsWith("http://dx.doi.org/10.1017/is") || url.startsWith("http://dx.doi.org/10.1051/S") || url.startsWith("http://dx.doi.org/10.1112/S0010437X") || url.startsWith("http://dx.doi.org/10.1112/S14611570") || url.startsWith("http://dx.doi.org/10.1112/S00255793") => {
            val regex = """<a href="([^"]*)"[ \t\n]*title="View PDF" class="article-pdf">""".r
            regex.findFirstMatchIn(HttpClientSlurp(url).mkString("\n")).map(m => "http://journals.cambridge.org/action/" + m.group(1).replaceAll("\n", "").replaceAll("\t", "").replaceAll(" ", ""))
          }

          // Wiley
          // 10.1002/(SICI)1097-0312(199602)49:2<85::AID-CPA1>3.0.CO;2-2 ---resolves to---> http://onlinelibrary.wiley.com/doi/10.1002/(SICI)1097-0312(199602)49:2%3C85::AID-CPA1%3E3.0.CO;2-2/abstract
          // 															 ---links to--->    http://onlinelibrary.wiley.com/doi/10.1002/(SICI)1097-0312(199602)49:2%3C85::AID-CPA1%3E3.0.CO;2-2/pdf
          //															 ---???---> http://onlinelibrary.wiley.com/store/10.1002/(SICI)1097-0312(199602)49:2%3C85::AID-CPA1%3E3.0.CO;2-2/asset/1_ftp.pdf?v=1&t=hfc3fjoo&s=dc6fad69f11cfc2ff2f302f5d1386c553d48f47c
          // the mystery step here is somewhat strange; it looks like it contains an iframe, but then redirects (via javascript) to the contents of the iframe?
          // anyway, the following scraping seems to work
          case url if url.startsWith("http://dx.doi.org/10.1002/") => {
            val regex = """id="pdfDocument" src="([^"]*)"""".r
            val url2 = "http://onlinelibrary.wiley.com/doi/" + url.stripPrefix("http://dx.doi.org/") + "/pdf"
            val slurped = HttpClientSlurp(url2).mkString("\n")
            regex.findFirstMatchIn(slurped).map(m => m.group(1))
          }

          // ACM
          case url if url.startsWith("http://dx.doi.org/10.1145/") => {
            val regex = """title="FullText Pdf" href="(ft_gateway\.cfm\?id=[0-9]*&type=pdf&CFID=[0-9]*&CFTOKEN=[0-9]*)"""".r
            regex.findFirstMatchIn(HttpClientSlurp.getString("http://dl.acm.org/citation.cfm?doid=" + url.drop(26))).map(m => m.group(1)).map("http://dl.acm.org/" + _)
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
          case url if url.startsWith("http://aif.cedram.org/item?id=") => {
            Some(url.replaceAllLiterally("http://aif.cedram.org/item?id=", "http://aif.cedram.org/cedram-bin/article/") + ".pdf")
          }
          case url if url.startsWith("http://muse.jhu.edu/journals/american_journal_of_mathematics/") => Some(url)
        }
      }
    }
  }

  def pdfInputStream: Option[InputStream] = {
    if (DOI.nonEmpty && DOI.get.startsWith("10.1215")) {
      // Duke expects to see a Referer field.
      pdfURL.map({ u =>
        HttpClientSlurp.getStream(u, referer = Some(u))
      })
    } else {
      pdfURL.map(HttpClientSlurp.getStream)
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
          val result = Some(IOUtils.toByteArray(bis))
          bis.close
          result
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

  val defaultFilenameTemplate = "$TITLE - $AUTHOR - $JOURNALREF - $MRNUMBER.pdf"

  def constructFilename(filenameTemplate: String = defaultFilenameTemplate) = {
    val authorNames = authors.map(a => pandoc.latexToText(a.name))
    val textCitation = pandoc.latexToText(citation)

    def preprocessAccents(s: String) = {
      s.replaceAllLiterally("""\Dbar""", "Đ")
        .replaceAllLiterally("""\soft{L}""", "Ľ")
        .replaceAllLiterally("""\cfac""", """\~""")
        .replaceAllLiterally("""\cftil{e}""", "ễ")
        .replaceAllLiterally("""\cftil{o}""", "ỗ")
        .replaceAllLiterally("/", "⁄") // scary UTF-8 character that just *looks* like a forward slash
        .replaceAllLiterally(":", "꞉") // scary UTF-8 character that just *looks* like a colon
    }

    def stripMoreLaTeX(s: String) = {
      val r = "\\{\\\\(rm|bf|scr|Bbb|bold) ([A-Za-z]*)\\}".r

      r.replaceAllIn(s, m => m.group(2))
        .replaceAllLiterally("""\ast""", "*")
        .replaceAllLiterally("""\bold """, "")
        .replaceAllLiterally("""\bf """, "")
        .replaceAllLiterally("""\Bbb """, "")
        .replaceAllLiterally("""\scr """, "")
        .replaceAllLiterally("""\rm """, "")
    }

    val textTitle = stripMoreLaTeX(pandoc.latexToText(preprocessAccents(title).replaceAll("""\[[^]]*MR[^]]*\]""", "").replaceAll("""\[[^]]*refcno[^]]*\]""", "")))

    ({
      val attempt = filenameTemplate
        .replaceAllLiterally("$TITLE", textTitle)
        .replaceAllLiterally("$AUTHOR", authorNames.mkString(" and "))
        .replaceAllLiterally("$JOURNALREF", textCitation)
        .replaceAllLiterally("$MRNUMBER", identifierString)
      if (attempt.length > 250) {
        val shortAuthors = if (authors.size > 4) {
          authorNames.head + " et al."
        } else {
          authorNames.mkString(" and ")
        }
        val shortCitation = if (textCitation.length > 95) {
          textCitation.take(92) + "..."
        } else {
          textCitation
        }
        val partialReplacement = filenameTemplate
          .replaceAllLiterally("$AUTHOR", shortAuthors)
          .replaceAllLiterally("$JOURNALREF", shortCitation)
          .replaceAllLiterally("$MRNUMBER", identifierString)
        val maxTitleLength = 250 - (partialReplacement.length - 6)
        val shortTitle = if (textTitle.length > maxTitleLength) {
          textTitle.take(maxTitleLength - 3) + "..."
        } else {
          textTitle
        }
        partialReplacement.replaceAllLiterally("$TITLE", shortTitle).ensuring(_.length <= 250)
      } else {
        attempt
      }
    })
  }

  def savePDF(directory: File, filenameTemplate: String = defaultFilenameTemplate) {
    val fileName = constructFilename(filenameTemplate)
    val file = new File(directory, fileName)
    if (directory.listFiles(new FilenameFilter { override def accept(dir: File, name: String) = name.contains(identifierString) }).nonEmpty) {
      Logging.info("PDF for " + identifierString + " already exists in " + directory)
      // TODO this shouldn't really be here:
      //      CanonicalizePDFNamesApp.safeRename(identifierString, directory, fileName)
    } else {
      pdf match {
        case Some(bytes) => {
          println("Saving PDF to " + fileName)
          FileUtils.writeByteArrayToFile(file, bytes)
        }
        case None => {
          Logging.info("No PDF available for " + fileName)
//          ???
        }
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

  val ElsevierSlurpCache = {
    import net.tqft.toolkit.functions.Memo._
    { url: String => HttpClientSlurp.apply(url).toList }.memo
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

  def fromBibtexGzipFile(file: String): Iterator[Article] = {
    import net.tqft.toolkit.collections.Split._
    Source.fromInputStream(new GZIPInputStream(new FileInputStream(file))).getLines.splitOn(_.isEmpty).map(_.mkString("\n")).grouped(100).flatMap(_.par.flatMap(Article.fromBibtex))
  }

  def withDOIPrefix(prefix: String): Iterator[Article] = {
    val pool = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(50))
    for (group <- AnonymousS3("DOI2mathscinet").keysWithPrefix(prefix).iterator.grouped(1000); doi <- { val p = group.par; p.tasksupport = pool; p }; article <- Article.fromDOI(doi)) yield article
  }

}