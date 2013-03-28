package net.tqft.mathscinet
import net.tqft.util.Slurp
import net.tqft.util.URLEncode
import net.tqft.arxiv.arXiv
import net.tqft.util.BIBTEX

trait Article {
  def identifier: Int
  def identifierString = "MR" + ("0000000" + identifier.toString).takeRight(7)

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
      val text = BIBTEX.cache.getOrElseUpdate(identifierString, {
        val lines = Slurp(bibtexURL).toList
        val start = lines.indexWhere(_.trim == "<pre>")
        val finish = lines.indexWhere(_.trim == "</pre>")
        lines.slice(start + 1, finish).mkString("\n")
      })
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

  def fromBibtex(bibtexString: String): Option[Article] = {
    BIBTEX.parse(bibtexString).map({
      case b @ BIBTEX(_, identifierString @ MRIdentifier(id), data) =>
        b.save
        val result = new Article {
          override val identifier = id
        }
        result.bibtexData = Some(b)
        result
    })
  }
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
}