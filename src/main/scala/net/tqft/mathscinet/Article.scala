package net.tqft.mathscinet
import net.tqft.util.Slurp
import net.tqft.util.URLEncode
import net.tqft.arxiv.arXiv
import net.tqft.util.BIBTEX
import java.io.File
import scala.io.Source
import net.tqft.toolkit.amazon.AnonymousS3
import net.tqft.toolkit.Extractors.Int

trait Article {
  def identifier: Int
  def identifierString = "MR" + ("0000000" + identifier.toString).takeRight(7)
  def shortIdentifierString = "MR" + identifier.toString

  def URL = "http://www.ams.org/mathscinet-getitem?mr=" + identifier
  def bibtexURL = "http://www.ams.org/mathscinet/search/publications.html?fmt=bibtex&pg1=MR&s1=" + identifier
  def endnoteURL = "http://www.ams.org/mathscinet/search/publications.html?fmt=endnote&pg1=MR&s1=" + identifier

  lazy val slurp = Slurp(URL).toList

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
    bibtex.get("DOI")

    //    bibtexData match {
    //      case Some(data) => data.get("DOI")
    //      case None => {
    //        val re = """<a .* href="/leavingmsn\?url=http://dx.doi.org/([^"]*)">Article</a>""".r
    //
    //        (slurp.flatMap { line =>
    //          line.trim match {
    //            case re(doi) => Some(doi)
    //            case _ => None
    //          }
    //        }).headOption
    //      }
    //    }
  }

}

object Article {
  def apply(identifier: String): Article = {
    apply(identifier.stripPrefix("MR").toInt)
  }

  def apply(identifier: Int): Article = {
    val identifier_ = identifier
    new Article {
      override val identifier = identifier_
    }
  }

  def fromDOI(doi: String): Option[Article] = {
    AnonymousS3("DOI2mathscinet").get(doi).map(apply)
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

}