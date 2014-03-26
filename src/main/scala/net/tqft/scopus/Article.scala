package net.tqft.scopus

import net.tqft.util.BIBTEX
import net.tqft.util.Slurp
import net.tqft.toolkit.Logging
import net.tqft.citationsearch.CitationScore
import net.tqft.toolkit.Extractors._

case class Article(id: String, title: String) {
  def URL = "http://www.scopus.com/record/display.url?eid=" + id + "&origin=resultslist"
  def textURL = "http://www.scopus.com/onclick/export.url?oneClickExport=%7b%22Format%22%3a%22TEXT%22%2c%22View%22%3a%22FullDocument%22%7d&origin=recordpage&eid=" + id + "&zone=recordPageHeader&outputType=export"
  def bibtexURL = "http://www.scopus.com/onclick/export.url?oneClickExport=%7b%22Format%22%3a%22BIB%22%2c%22View%22%3a%22FullDocument%22%7d&origin=recordpage&eid=" + id + "&zone=recordPageHeader&outputType=export"

  lazy val dataText = Slurp(textURL).toStream

  private def dataWithPrefix(prefix: String) = dataText.find(_.startsWith(prefix + ": ")).map(_.stripPrefix(prefix + ": "))

  def citation = "(.*) Cited [0-9]* times?.".r.findFirstMatchIn(dataText(5).trim).map(_.group(1)).getOrElse(dataText(5))
  def ISSNOption = dataWithPrefix("ISSN").map(s => s.take(4) + "-" + s.drop(4))
  def DOIOption = dataWithPrefix("DOI")
  def authorData = dataText(3)
  def yearOption = """^\(([0-9]*)\) """.r.findFirstMatchIn(citation).map(_.group(1)).collect({ case Int(i) => i })

  def numberOfCitations: Option[Int] = ".* Cited ([0-9]*) times?.".r.findFirstMatchIn(dataText(5).trim).map(_.group(1).toInt)

  def fullCitation = title + " - " + authorData + " - " + citation + " - scopus:" + id
  lazy val matches = net.tqft.citationsearch.Search.query(title + " - " + authorData + " - " + citation + DOIOption.map(" " + _).getOrElse("")).results
  
  lazy val satisfactoryMatch: Option[CitationScore] = {
    matches.headOption.filter(s => s.score > 0.85).orElse(
      matches.sliding(2).filter(p => p(0).score > 0.42 && scala.math.pow(p(0).score, 1.6) > p(1).score).toStream.headOption.map(_.head))
  }

  def references = {
    import net.tqft.toolkit.collections.TakeToFirst._
    dataText.iterator.dropWhile(!_.startsWith("REFERENCES: ")).map(_.stripPrefix("REFERENCES: ").trim).takeToFirst(!_.endsWith(";")).map(_.stripSuffix(";")).toSeq
  }

  lazy val referenceMatches = references.iterator.toStream.map(r => (r, net.tqft.citationsearch.Search.query(r).results))

}