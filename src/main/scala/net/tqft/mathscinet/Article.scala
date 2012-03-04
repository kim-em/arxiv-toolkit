package net.tqft.mathscinet
import scala.io.Source

trait Article {
  def identifier: Int

  def URL = "http://www.ams.org/mathscinet-getitem?mr=" + identifier

  lazy val slurp = Source.fromURL(URL).getLines.toList

  def DOI: Option[String] = {    
    val re = """<a .* href="/leavingmsn\?url=http://dx.doi.org/([^"]*)">Article</a>""".r

    (slurp.flatMap { line =>
      line.trim match {
        case re(doi) => Some(doi)
        case _ => None
      }
    }).headOption
  }
}

object Article {
  def apply(identifier: Int): Article = {
    val identifier_ = identifier
    new Article {
      override val identifier = identifier_
    }
  }
}