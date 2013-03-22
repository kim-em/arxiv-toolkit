package net.tqft.mathscinet
import net.tqft.util.Slurp
import net.tqft.util.URLEncode
import net.tqft.arxiv.arXiv
import net.tqft.util.BIBTEX

trait Article {
  def identifier: Int

  def URL = "http://www.ams.org/mathscinet-getitem?mr=" + identifier
  def bibtexURL = "http://www.ams.org/mathscinet/search/publications.html?fmt=bibtex&pg1=MR&s1=" + identifier
  def endnoteURL = "http://www.ams.org/mathscinet/search/publications.html?fmt=endnote&pg1=MR&s1=" + identifier

  lazy val slurp = Slurp(URL).toList

  var endnoteData: Option[Map[String, List[String]]] = None
  var bibtexData: Option[BIBTEX] = None

  def endnote = {
    while (endnoteData.isEmpty) {
      val lines = Slurp(endnoteURL).toList
      val start = lines.indexWhere(_.trim == "<pre>")
      val finish = lines.indexWhere(_.trim == "</pre>")
      endnoteData = Some(lines.slice(start + 1, finish).groupBy(_.take(2)).mapValues(l => l.map(_.drop(3))))
    }
    endnoteData.get
  }
  def bibtex = {
    if (bibtexData.isEmpty) {
      val lines = Slurp(bibtexURL).toList
      val start = lines.indexWhere(_.trim == "<pre>")
      val finish = lines.indexWhere(_.trim == "</pre>")
      bibtexData = BIBTEX.parse(lines.slice(start + 1, finish).mkString("\n"))
    }
    bibtexData.get
  }

  // FIXME load from endnote or bibtex
  def title: String = endnote("%T").head
  def authors: List[Author] = endnote("%A").map(Author(_))
  def journalReference: String = endnote("%J").head + " " + endnote("%V").head + " (" + endnote("%D").head + "), no. " + endnote("%N").head + ", " + endnote("%P").head

  def DOI: Option[String] = {
    bibtexData match {
      case Some(data) => data.get("DOI")
      case None => {
        val re = """<a .* href="/leavingmsn\?url=http://dx.doi.org/([^"]*)">Article</a>""".r

        (slurp.flatMap { line =>
          line.trim match {
            case re(doi) => Some(doi)
            case _ => None
          }
        }).headOption
      }
    }
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
      case b @ BIBTEX(_, identifierString, data) =>
        val identifier_ = identifierString.stripPrefix("MR").toInt
        val result = new Article {
          override val identifier = identifier_
        }
        result.bibtexData = Some(b)
        result
    })
  }
}