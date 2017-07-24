package net.tqft.arxiv2

import net.tqft.citationsearch.CitationScore
import net.tqft.util.pandoc
import net.tqft.zentralblatt.CitationMatching
import net.tqft.util.Slurp
import net.tqft.citation.Citation
import java.io.PrintWriter
import java.io.FileOutputStream
import java.io.File
import scala.io.Source

object SourcesApp extends App {
  //  for ((key, text, o) <- Sources.referencesResolved("math/9912028")) {
  //    println(pandoc.latexToText(text))
  //    o.map({
  //      case CitationScore(citation, score) =>
  //        println(citation.best)
  //        println(citation.title + " - " + citation.authors + " - " + citation.citation_text)
  //    })
  //    println()
  //  }
  //  for(s <- Sources.bibitems("math/9912028")) {
  //    println(s)
  //    println
  //  }
  var targets = scala.collection.mutable.ListBuffer[String]()
  val done = scala.collection.mutable.ListBuffer[String]()
  def randomTarget = targets.apply(scala.util.Random.nextInt(targets.size))

  val graph = new File(System.getProperty("user.home") + "/projects/arxiv-toolkit/graph")
  if (graph.exists) {
    for (line <- Source.fromFile(graph).getLines; h :: t = line.split(' ').toList) {
      done += h
      targets ++= t
      targets = targets.filterNot(_ == h)
    }
  }

  targets ++= (Seq("1606.03466", "0809.3031").filterNot(c => done.contains(c)))

  val pw = new PrintWriter(new FileOutputStream(graph, true))

  while (targets.nonEmpty) {
    println(s"Completed ${done.size} articles.")
    val target = randomTarget
    targets = targets.filterNot(_ == target)
    done += target
    try {
      val cites = (for (s <- Sources.bibitems(target)) yield {
        println(s)
        val cites = Citation(s._2).arxiv
        for (a <- cites.headOption) {
          println("-- " + a.identifier + ": " + a.citation)
          if (!done.contains(a.identifier)) {
            targets += a.identifier
          }
        }
        println
        cites.map(_.identifier)
      }).flatten
      pw.write((target +: cites).mkString(" ") + "\n")
      pw.flush
    } catch {
      case e: Exception => {
        println("Something went wrong while reading the bibliography of " + target)
        println(e.getMessage)
        e.printStackTrace()
      }
    }
  }

  pw.close

  Slurp.quit
}