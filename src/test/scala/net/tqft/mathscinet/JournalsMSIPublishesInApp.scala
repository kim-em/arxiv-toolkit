package net.tqft.mathscinet

import java.io.File
import net.tqft.util.CSVParser
import net.tqft.toolkit.Extractors.Int
import scala.io.Codec
import net.tqft.util.FirefoxSlurp
import scala.collection.mutable.{ Map => mMap }
import scala.io.Source
import net.tqft.journals.Journals
import scala.collection.parallel.ForkJoinTaskSupport

object JournalsMSIPublishesInApp extends App {

  Article.disableBibtexSaving

  val mathematicians = (for (
    line <- io.Source.fromFile(new File(System.getProperty("user.home") + "/projects/arxiv-toolkit/mathematicians.txt"))(Codec.UTF8).getLines;
    if line.nonEmpty && !line.startsWith("#");
    fields = CSVParser(line)
  ) yield fields).toList

  val authors = for (
    Int(mathscinetAuthorId) :: _ :: name :: "ANU" :: level :: _ <- mathematicians;
    if mathscinetAuthorId > 0
  ) yield {
    Author(mathscinetAuthorId, name)
  }

  val pool = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(50))

  val articles = { val p = authors.par; p.tasksupport = pool; p }
    .flatMap(_.articles)

  val journalCounts = articles
    .filter(a => a.yearOption.nonEmpty && a.year > 2000)
    .groupBy(_.ISSNOption.getOrElse(""))
    .mapValues(articles => (articles.head.journalOption, articles.size)).seq - ""

  val ERA = Source.fromURL("http://tqft.net/math/ERA2015-issns.txt").getLines.toSeq

  val articlesNotInERAJournals = articles.filter(a => a.ISSNOption.nonEmpty && !ERA.contains(a.ISSN))

  for ((issn, (Some(name), count)) <- journalCounts -- ERA) {
    println(issn + " " + name + " " + count)
  }

  for (a <- articlesNotInERAJournals; if a.yearOption.nonEmpty && a.year > 2000) {
    println(a.bibtex.toBIBTEXString)
  }

  FirefoxSlurp.quit
}