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
    Int(id) :: name :: "ANU" :: level :: _ <- mathematicians;
    if id > 0;
    if level == "E" || level == "D"
  ) yield {
    Author(id, name)
  }

  val pool = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(50))

  val journalCounts = { val p = authors.par; p.tasksupport = pool; p }
    .flatMap(_.articles)
    .filter(a => a.yearOption.nonEmpty && a.year > 2000)
    .groupBy(_.ISSNOption.getOrElse(""))
    .mapValues(articles => (articles.head.journalOption, articles.size)).seq - ""

  val ERA = Source.fromURL("http://tqft.net/math/ERA2015-issns.txt").getLines.toSeq

  for ((issn, (Some(name), count)) <- journalCounts -- ERA) {
    println(issn + " " + name + " " + count)
  }

  FirefoxSlurp.quit
}