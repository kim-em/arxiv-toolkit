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

object SortByScopusOnlyCitations extends App {

  val mathematicians = (for (
    line <- io.Source.fromFile(new File(System.getProperty("user.home") + "/projects/arxiv-toolkit/mathematicians.txt"))(Codec.UTF8).getLines;
    if line.nonEmpty && !line.startsWith("#");
    fields = CSVParser(line)
  ) yield fields).toList

  val authors = for (
    Int(mathscinetAuthorId) :: Long(scopusAuthorId) :: name :: "ANU" :: level :: _ <- mathematicians;
    if mathscinetAuthorId != 0;
    if scopusAuthorId != 0
  //    if mathscinetAuthorId == 40355 //Peter
  //    if mathscinetAuthorId == 32465
  ) yield {
    (Author(mathscinetAuthorId, name), net.tqft.scopus.Author(scopusAuthorId, name))
  }

  val firstYear = 2005

  val counts = for ((ma, sa) <- authors) yield {
    val recentPublicationsOnScopus = sa.publications.filter(a => a.yearOption.nonEmpty && a.yearOption.get >= firstYear)
    val recentPublicationsOnMathSciNet = ma.articles.filter(a => a.yearOption.nonEmpty && a.yearOption.get >= firstYear).toStream

    val tentativeMatches = recentPublicationsOnScopus.map(p => (p, p.satisfactoryMatch)).collect({
      case (p, Some(CitationScore(c, _))) if c.MRNumber.nonEmpty => (p, Article(c.MRNumber.get))
    })
    val matches = tentativeMatches.groupBy(_._2.identifier).filter(_._2.size == 1).map(_._2.head)

    var onlyOnMathSciNet = 0
    var onlyOnScopus = 0

    for ((a1, a2) <- matches) {

      val citations1 = a2.citations.toSeq
      val candidateMatches = a1.bestCitationMathSciNetMatches
      val goodMatches = candidateMatches.filter(m => m._2.nonEmpty && citations1.contains(m._2.get)).map(p => (p._1, p._2.get))
      val failedMatches = candidateMatches.filter(m => m._2.isEmpty || !citations1.contains(m._2.get)).map(_._1)
      val unmatched = citations1.filterNot(r => candidateMatches.exists(_._2 == Some(r)))

      onlyOnScopus = onlyOnScopus + failedMatches.size
      onlyOnMathSciNet = onlyOnMathSciNet + unmatched.size
    }

    (ma.firstNameLastName, onlyOnScopus, onlyOnMathSciNet)
  }

  for (p <- counts.sortBy(_._2)) println(p)

  FirefoxSlurp.quit

}