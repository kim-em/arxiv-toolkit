package net.tqft.scopus

import net.tqft.util.FirefoxSlurp
import java.io.File
import scala.io.Codec
import net.tqft.util.CSVParser
import net.tqft.toolkit.Extractors.Long

object AuthorApp extends App {

  val mathematicians = (for (
    line <- io.Source.fromFile(new File(System.getProperty("user.home") + "/projects/arxiv-toolkit/mathematicians.txt"))(Codec.UTF8).getLines;
    if line.nonEmpty && !line.startsWith("#");
    fields = CSVParser(line)
  ) yield fields).toList

  val authors = for (
    _ :: Long(id) :: name :: university :: level :: _ <- mathematicians;
    if id > 0
  ) yield {
    Author(id, name)
  }

  for (author <- authors) {

    val publications = author.publications.toStream
    for (p <- publications) {
      println(p)
    }

    for (p <- publications.headOption) {
      println(p.dataText.mkString("\n"))
      println(p.citation)
      p.numberOfCitations.map(n => println(s"Cited $n times."))

      for (r <- p.matches) {
        println(r)
      }

      for ((r, m) <- p.referenceMatches) {
        println(r)
        if (m(0).score > m(1).score * 2) {
          println("   " + m(0).citation)
        }
      }
    }

  }
  FirefoxSlurp.quit
}