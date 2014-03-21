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

object CompareScopusAndMathSciNetApp extends App {
  
  val outputFile = new File(System.getProperty("user.home") + "/projects/arxiv-toolkit/compare.txt")
  outputFile.delete
  val out = new PrintWriter(new FileOutputStream(outputFile))
  def p(s: String) = {
    println(s)
    out.println(s)
    out.flush
  }
  
  
  val mathematicians = (for (
    line <- io.Source.fromFile(new File(System.getProperty("user.home") + "/projects/arxiv-toolkit/mathematicians.txt"))(Codec.UTF8).getLines;
    if line.nonEmpty && !line.startsWith("#");
    fields = CSVParser(line)
  ) yield fields).toList

  val authors = for (
    Int(mathscinetAuthorId) :: Long(scopusAuthorId) :: name :: "ANU" :: level :: _ <- mathematicians;
    if mathscinetAuthorId > 0 && scopusAuthorId > 0
  ) yield {
    (Author(mathscinetAuthorId, name), net.tqft.scopus.Author(scopusAuthorId, name))
  }

  val firstYear = 2005
  
  for ((ma, sa) <- authors) {
    out.println("Analyzing publications for " + ma.name)

    val recentPublicationsOnScopus = sa.publications.filter(a => a.yearOption.nonEmpty && a.yearOption.get >= firstYear)
    val recentPublicationsOnMathSciNet = ma.articles.filter(a => a.yearOption.nonEmpty && a.yearOption.get >= firstYear).toStream
    
    val onlyOnScopus = recentPublicationsOnScopus.filter(_.satisfactoryMatch.isEmpty)
    val matches = sa.publications.map(p => (p, p.satisfactoryMatch)).collect({
      case (p, Some(CitationScore(c, _))) if c.MRNumber.nonEmpty => (p, Article(c.MRNumber.get))
    })
    lazy val matchedMathSciNetIds = matches.map(_._2.identifier).toSet
    lazy val onlyOnMathSciNet = recentPublicationsOnMathSciNet.filterNot(a => matchedMathSciNetIds.contains(a.identifier))

    p("  Articles found only on Scopus:")
    for (a <- onlyOnScopus) {
      p("    " + a.fullCitation)
      for(m <- a.matches.headOption)
      p(s"      best match (${m.score}): ${m.citation.title} - ${m.citation.authors} - ${m.citation.cite} ${m.citation.MRNumber.map(n => "- MR" + n).getOrElse("")}")
    }
    p("   ============================")
    p("   Matching articles found:")
    for ((a1, a2) <- matches) {
      p("    " + a1.fullCitation)
      p("    " + a2.fullCitation)
      p("    --------------")
    }
    p("   ============================")
    p("   Articles found only on MathSciNet:")
    for (a <- onlyOnMathSciNet) {
      p("    " + a.fullCitation)
    }
    p("   ============================")
  }

  FirefoxSlurp.quit
  
}