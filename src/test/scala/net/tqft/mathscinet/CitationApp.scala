package net.tqft.mathscinet

import java.io.File
import net.tqft.util.CSVParser
import net.tqft.toolkit.Extractors.Int
import scala.io.Codec
import net.tqft.util.FirefoxSlurp
import scala.collection.mutable
import net.tqft.util.Slurp
import net.tqft.toolkit.amazon.S3

object CitationApp extends App {

  //  S3("hIndex1996").clear
  //  S3("hIndex2008").clear

  Article.disableBibtexSaving

  val mathematicians = (for (
    line <- io.Source.fromFile(new File(System.getProperty("user.home") + "/projects/arxiv-toolkit/mathematicians.txt"))(Codec.UTF8).getLines;
    if line.nonEmpty && !line.startsWith("#");
    fields = CSVParser(line)
  ) yield fields).toList

  val authors = for (
    Int(id) :: _ :: name :: university :: level :: _ <- mathematicians;
    if id > 0
  ) yield {
    (Author(id, name), university, level)
  }

  type Level = String
  type Year = Int

  val counts = mutable.Map[Level, mutable.Map[Year, mutable.Map[Int, Int]]]()
  val years = Seq(2008, 1996)
  val levels = Seq("?", "A", "B", "C", "D", "E")

  for (level <- levels) {
    counts(level) = mutable.Map[Year, mutable.Map[Int, Int]]()
    for (year <- years) {
      counts(level)(year) = mutable.Map[Int, Int]()
    }
  }

  Slurp.overwriteCache = true

    try {
      for ((a, uni, level) <- authors) {
        for (year <- years)
          counts(level)(year)(a.hIndex(year)) = counts(level)(year).getOrElseUpdate(a.hIndex(year), 0) + 1
        println(a.name + s" ($uni, $level) " + years.map(a.hIndex).mkString(" "))
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
        System.exit(1)
    }

//  try {
//    for ((a, uni, level) <- authors) {
//      for (h <- Author.hIndexCache2008.get(a.id)) {
//        counts(level)(2008)(h) = counts(level)(2008).getOrElse(h, 0) + 1
//        println(a.name + s" ($uni, $level) " + h)
//      }
//      for (h <- Author.hIndexCache1996.get(a.id)) {
//        counts(level)(1996)(h) = counts(level)(1996).getOrElse(h, 0) + 1
//        println(a.name + s" ($uni, $level) " + h)
//      }
//    }
//  } catch {
//    case e: Exception =>
//      e.printStackTrace()
//      System.exit(1)
//  }

  for (year <- years) {
    println(year)
    for (level <- levels) {
      println(level)
      println(counts(level)(year).toSeq.sorted)
    }
  }

  println(counts)

  FirefoxSlurp.quit
}