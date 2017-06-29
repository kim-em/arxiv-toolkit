package net.tqft.webofscience

import java.io.File
import scala.io.Codec
import net.tqft.util.CSVParser
import net.tqft.toolkit.Extractors.Int
import net.tqft.toolkit.Extractors.Long

object FindAccessionNumbersApp extends App {
  val mathematicians = (for (
    line <- scala.io.Source.fromFile(new File(System.getProperty("user.home") + "/projects/arxiv-toolkit/mathematicians.txt"))(Codec.UTF8).getLines;
    if line.nonEmpty && !line.startsWith("#");
    fields = CSVParser(line)
  ) yield fields).toList

  val authors = for (
    Int(mathscinetAuthorId) :: Long(scopusAuthorId) :: name :: "ANU" :: level :: _ <- mathematicians
  ) yield {
    (net.tqft.mathscinet.Author(mathscinetAuthorId, name), net.tqft.scopus.Author(scopusAuthorId, name))
  }

  for (
    (author, _) <- authors;
    article <- author.articles
  ) {
    println(Article.fromMathSciNet(article, useGoogleScholar = true))
  }

}