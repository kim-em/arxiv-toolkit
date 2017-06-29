package net.tqft.zentralblatt

import net.tqft.util.Slurp
import argonaut._
import Argonaut._

object CitationMatching {
  implicit val codec1 = CodecJson.derive[Zbl]
  implicit val codec2 = CodecJson.derive[CitationMatchingResults]

  def apply(query: String): List[Zbl] = {
    val rawJson = Slurp("https://zbmath.org/citationmatching/mathoverflow?q=" + query).toStream.init.tail.mkString("{\n","\n","\n}\n")
    rawJson.decodeOption[CitationMatchingResults].get.results
  }
}

case class CitationMatchingResults(results: List[Zbl])

case class Zbl(
    title: String, 
    authors: String,
    source: String,
    year: Int,
    pagination: String, 
    zbl_id: String,
    score: Double,
    links: List[String]) {
  def doi: Option[String] = links.find(l => l.startsWith("http://dx.doi.org/")).map(_.stripPrefix("http://dx.doi.org/"))
  def arxiv: Option[String] = links.find(l => l.startsWith("http://www.arxiv.org/abs/")).map(_.stripPrefix("http://www.arxiv.org/abs/"))
}