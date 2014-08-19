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
    Int(mathscinetAuthorId) :: Long(scopusAuthorId) :: name :: "ANU" :: level :: _ <- mathematicians//;
//    if mathscinetAuthorId == 40355 //Peter
//    if mathscinetAuthorId == 32465
  ) yield {
    (Author(mathscinetAuthorId, name), net.tqft.scopus.Author(scopusAuthorId, name))
  }

  val firstYear = 2005


  val counts = for ((ma, sa) <- authors) yield {
     val recentPublicationsOnScopus = sa.publications.filter(a => a.yearOption.nonEmpty && a.yearOption.get >= firstYear)
     val recentPublicationsOnMathSciNet = ma.articles.filter(a => a.yearOption.nonEmpty && a.yearOption.get >= firstYear).toStream

     val onlyOnScopus = recentPublicationsOnScopus.filter(_.satisfactoryMatch.isEmpty)
    
     (ma.firstNameLastName, onlyOnScopus.size)
  }

  for(p <- counts.sortBy(_._2)) println(p)
  
  FirefoxSlurp.quit

}