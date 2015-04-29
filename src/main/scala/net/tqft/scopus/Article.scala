package net.tqft.scopus

import net.tqft.util.BIBTEX
import net.tqft.util.Slurp
import net.tqft.toolkit.Logging
import net.tqft.citationsearch.CitationScore
import net.tqft.toolkit.Extractors._
import net.tqft.util.pandoc

case class Article(id: String, titleHint: Option[String] = None) {
  def URL = "http://www.scopus.com/record/display.url?eid=" + id + "&origin=resultslist"
  def textURL = "http://www.scopus.com/onclick/export.url?oneClickExport=%7b%22Format%22%3a%22TEXT%22%2c%22View%22%3a%22FullDocument%22%7d&origin=recordpage&eid=" + id + "&zone=recordPageHeader&outputType=export"
  def bibtexURL = "http://www.scopus.com/onclick/export.url?oneClickExport=%7b%22Format%22%3a%22BIB%22%2c%22View%22%3a%22FullDocument%22%7d&origin=recordpage&eid=" + id + "&zone=recordPageHeader&outputType=export"
  def citationsURL(page: Int) = "http://www.scopus.com/results/results.url?cc=10&sort=plf-f&cite=" + id + "&src=s&nlo=&nlr=&nls=&imp=t&sot=cite&sdt=a&sl=0&ss=plf-f&ps=r-f&" + (if (page > 0) { "offset=" + (20 * page + 1) + "&" } else { "" }) + "origin=resultslist&zone=resultslist"
  // http://www.scopus.com/results/results.url?cc=10&sort=plf-f&cite=2-s2.0-46049118990&src=s&nlo=&nlr=&nls=&imp=t&sot=cite&sdt=a&sl=0&ss=plf-f&ps=r-f&origin=resultslist&zone=resultslist

  // http://www.scopus.com/results/results.url?sort=plf-f&src=s&sot=aut&sdt=a&sl=17&s=AU-ID%287004335929%29

  def getDataText: Stream[String] = {
    val slurp = Slurp(textURL).toStream
    if (slurp.mkString("\n").contains("getElementById")) {
      Logging.warn("Corrupted metadata for " + id, " clearing cache and trying again.")
      Slurp -= textURL
      getDataText
    } else {
      slurp
    }
  }

  lazy val dataText = getDataText

  private def dataWithPrefix(prefix: String) = dataText.find(_.startsWith(prefix + ": ")).map(_.stripPrefix(prefix + ": "))

  def title = titleHint.getOrElse(dataText(4))
  def citation = "(.*) Cited [0-9]* times?.".r.findFirstMatchIn(dataText(5).trim).map(_.group(1)).getOrElse(dataText(5))
  def ISSNOption = dataWithPrefix("ISSN").map(s => s.take(4) + "-" + s.drop(4))
  def DOIOption = dataWithPrefix("DOI")
  private def removeFootnotes(author: String): String = {
    val trimmed = author.trim
    if (trimmed.last.isLower && (trimmed.takeRight(2).head == '.' || trimmed.takeRight(2).head == ' ')) {
      removeFootnotes(trimmed.dropRight(2))
    } else {
      trimmed
    }
  }
  def authorData = {
    dataText(3)
  }
  def authors = authorData.split(",").map(removeFootnotes).sliding(2, 2).map(p => p(1) + ". " + p(0)).toSeq

  def authorsText = {
    import net.tqft.util.OxfordComma._
    authors.oxfordComma
  }

  lazy val authorAffiliationKeys = {
    val r = ".*?((?: *[a-z])*)".r
    authorData.split(" , ").toList.map(_.trim).map({ case a @ r(keys) => (a.stripSuffix(keys), keys.split(" ").toList.map(_.trim).filter(_.nonEmpty)) }).toMap
  }
  def authorAffiliations = authorAffiliationKeys.mapValues(v => v.map(c => affiliations("abcdefghijklmnopqrstuvwxyz".indexOf(c))))

  lazy val affiliations: Seq[String] = {
    import net.tqft.toolkit.collections.TakeToFirst._

    def separateCamelCase(s: String) = {
      val r = "([a-z][a-z])([A-Z][a-z])".r
      r.replaceAllIn(s, "\\1 \\2")
    }

    dataText.iterator.dropWhile(!_.startsWith("AFFILIATIONS: ")).map(_.stripPrefix("AFFILIATIONS: ").trim).takeToFirst(!_.endsWith(";")).map(_.stripSuffix(";").ensuring(_.nonEmpty)).toSeq.map(separateCamelCase)
  }

  def yearOption = """^\(([0-9]*)\) """.r.findFirstMatchIn(citation).map(_.group(1)).collect({ case Int(i) => i })

  def numberOfCitations: Option[Int] = ".* Cited ([0-9]*) times?.".r.findFirstMatchIn(dataText(5).trim).map(_.group(1).toInt)

  def fullCitationWithoutIdentifier = title + " - " + authorsText + " - " + citation
  def fullCitation = fullCitationWithoutIdentifier + " - scopus:" + id
  def fullCitation_html = title + " - " + authorsText + " - " + citation + " - <a href='" + URL + "'>scopus:" + id + "</a>"

  lazy val onWebOfScience: Option[net.tqft.webofscience.Article] = {
    DOIOption match {
      case Some(doi) => {
        net.tqft.webofscience.Article.fromDOI(doi)
      }
      case None => {
        net.tqft.scholar.Scholar(this).flatMap(r => r.webOfScienceAccessionNumber).map(an => net.tqft.webofscience.Article(an))
      }
    }
  }

  lazy val matches = net.tqft.citationsearch.Search.query(title + " - " + authorData + " - " + citation + DOIOption.map(" " + _).getOrElse("")).results.sortBy(-_.score)

  lazy val satisfactoryMatch: Option[CitationScore] = {
    matches.headOption.filter(s => s.score > 0.89).orElse(
      matches.sliding(2).filter(p => p(0).score > 0.48 && scala.math.pow(p(0).score, 1.75) > p(1).score).toStream.headOption.map(_.head))
  }

  private def read_citations: Seq[Article] = {
    val firstPage = Slurp(citationsURL(0)).toStream

    val totalCitationCountOption = "Scopus - ([0-9]*) documents? that cite".r.findFirstMatchIn(firstPage.mkString("\n")).map(_.group(1))
    if (totalCitationCountOption.isEmpty) {
      Logging.warn("Didn't find any citations on " + citationsURL(0))
      Slurp -= citationsURL(0)
      Thread.sleep(1000)
      read_citations
    } else {
      val totalCitationCount = totalCitationCountOption.get.toInt

      val allPages = for (i <- (0 until (totalCitationCount + 19) / 20).toStream; line <- Slurp(citationsURL(i))) yield line

      val r = "eid=([^&]*)&".r
      val result = allPages.flatMap(l => r.findFirstMatchIn(l).map(_.group(1))).distinct.filterNot(_ == id).map(i => Article(i))
      if (result.size != totalCitationCount) {
        Logging.warn("Scopus says there are " + totalCitationCount + " citations, but I see " + result.size)
      }
      result
    }

  }

  lazy val citations: Seq[Article] = read_citations

  lazy val citationMatches = citations.iterator.toStream.map(r => (r, net.tqft.citationsearch.Search.query(r.fullCitation).results))
  def bestCitationMathSciNetMatches = citationMatches.map({ p => (p._1, p._2.headOption.flatMap(_.citation.MRNumber).map(i => net.tqft.mathscinet.Article(i))) })

  lazy val references: Seq[String] = {
    import net.tqft.toolkit.collections.TakeToFirst._
    dataText.iterator.dropWhile(!_.startsWith("REFERENCES: ")).map(_.stripPrefix("REFERENCES: ").trim).takeToFirst(!_.endsWith(";")).map(_.stripSuffix(";").ensuring(_.nonEmpty)).toSeq
  }

  lazy val referenceMatches = references.iterator.toStream.map(r => (r, net.tqft.citationsearch.Search.query(r).results))

  def bestReferenceMathSciNetMatches = referenceMatches.map({ p => (p._1, p._2.headOption.flatMap(_.citation.MRNumber).map(i => net.tqft.mathscinet.Article(i))) })
}