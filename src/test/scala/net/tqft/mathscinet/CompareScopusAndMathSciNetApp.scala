package net.tqft.mathscinet

import java.io.File
import scala.io.Codec
import net.tqft.util.CSVParser
import net.tqft.toolkit.Extractors.Int
import net.tqft.toolkit.Extractors.Long
import net.tqft.citationsearch.CitationScore
import net.tqft.util.FirefoxSlurp
import java.io.PrintWriter
import java.io.FileOutputStream
import net.tqft.citationsearch.Citation
import java.io.OutputStreamWriter

object CompareScopusAndMathSciNetApp extends App {

  val outputFile = new File(System.getProperty("user.home") + "/projects/arxiv-toolkit/compare.html")
  outputFile.delete
  val out = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8")
  def p(s: String) = {
    println(s)
    out.write(s + "\n")
    out.flush
  }

  val mathematicians = (for (
    line <- io.Source.fromFile(new File(System.getProperty("user.home") + "/projects/arxiv-toolkit/mathematicians.txt"))(Codec.UTF8).getLines;
    if line.nonEmpty && !line.startsWith("#");
    fields = CSVParser(line)
  ) yield fields).toList

  val authors = for (
    Int(mathscinetAuthorId) :: Long(scopusAuthorId) :: name :: "ANU" :: level :: _ <- mathematicians
  ) yield {
    (Author(mathscinetAuthorId, name), net.tqft.scopus.Author(scopusAuthorId, name))
  }

  val firstYear = 2005

  def fullCitation(c: Citation) = {
    s"${c.title} - ${c.authors} - ${c.cite} ${c.MRNumber.map(n => "- MR" + n).getOrElse("")}"
  }

  p("""<!DOCTYPE html>
<html lang="en">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
<head>
<body>""")
  
  for ((ma, sa) <- authors) {
    p(s"<h2>Publications for <i>${ma.name}</i> since $firstYear</h2>")
    p(s"<div id='publications-${ma.id}'>")

    lazy val recentPublicationsOnScopus = sa.publications.filter(a => a.yearOption.nonEmpty && a.yearOption.get >= firstYear)
    lazy val recentPublicationsOnMathSciNet = ma.articles.filter(a => a.yearOption.nonEmpty && a.yearOption.get >= firstYear).toStream

    lazy val onlyOnScopus = recentPublicationsOnScopus.filter(_.satisfactoryMatch.isEmpty)
    lazy val matches = sa.publications.map(p => (p, p.satisfactoryMatch)).collect({
      case (p, Some(CitationScore(c, _))) if c.MRNumber.nonEmpty => (p, Article(c.MRNumber.get))
    })
    lazy val onlyOnMathSciNet = {
      if (sa.id > 0) {
        val matchedMathSciNetIds = matches.map(_._2.identifier).toSet
        recentPublicationsOnMathSciNet.filterNot(a => matchedMathSciNetIds.contains(a.identifier))
      } else {
        recentPublicationsOnMathSciNet
      }
    }

    if (sa.id > 0) {
      p("<h3>Articles found only on Scopus:</h3>")
      p("<dl>")
      for (a <- onlyOnScopus) {
        p("<dt>" + a.fullCitation + "</dt>")
        for (m <- a.matches.headOption)
          p(s"<dd>best match (${m.score}): ${fullCitation(m.citation)}</dd>")
      }
      p("</dl>")
      p("<h3>Matching articles found:</h3>")
      p("<dl>")
      for ((a1, a2) <- matches) {
        p("<dt>" + a1.fullCitation + "</dt>")
        p("<dd>" + a2.fullCitation + "</dd>")
      }
      p("</dl>")
    }
    if (ma.id > 0) {
      p("<h3>Articles found only on MathSciNet:</h3>")
      p("<ul>")
      for (a <- onlyOnMathSciNet) {
        p("<li>" + a.fullCitation + "</li>")
      }
      p("</ul>")
    }

    p("</div>")
  }

  p("</body></html>")
  FirefoxSlurp.quit

}