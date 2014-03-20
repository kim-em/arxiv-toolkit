package net.tqft.mathscinet

import java.io.File
import scala.io.Codec
import net.tqft.util.CSVParser
import net.tqft.toolkit.Extractors.Int
import net.tqft.toolkit.Extractors.Long
import net.tqft.citationsearch.CitationScore


object CompareScopusAndMathSciNetApp extends App {
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
  
  for((ma, sa) <- authors) {
    println("Analyzing publications for " + ma.name)
    
    val onlyOnScopus = sa.publications.filter(_.satisfactoryMatch.isEmpty)
    val matches =
    sa.publications.map(p => (p, p.satisfactoryMatch)).collect({
      case (p, Some(CitationScore(c, _))) if c.MRNumber.nonEmpty => (p, Article(c.MRNumber.get))
    })
    val onlyOnMathSciNet = ma.articles.filterNot(matches.map(_._2).contains)
    
    println("  Articles found only on Scopus:")
    for(a <- onlyOnScopus) {
      println("    " + a.fullCitation)
    }
    println("   ============================")
    println("   Matching articles found:")
    for((a1, a2) <- matches) {
      println("    " + a1.fullCitation)
      println("    " + a2.fullCitation)
      println("    --------------")
    }
    println("   ============================")
    println("   Articles found only on MathSciNet:")
    for(a <- onlyOnMathSciNet) {
      println("    " + a.fullCitation)
    }
    println("   ============================")
  }

}