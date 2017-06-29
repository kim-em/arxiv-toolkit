package net.tqft.arxiv2

import net.tqft.citationsearch.CitationScore
import net.tqft.util.pandoc
import net.tqft.zentralblatt.CitationMatching
import net.tqft.util.Slurp
import net.tqft.citation.Citation

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

  //  targets += "1606.03466"
  targets += "0809.3031"

  while (targets.nonEmpty) {
    println(s"Completed ${done.size} articles.")
    val target = randomTarget
    targets = targets.filterNot(_ == target)
    done += target
    for (s <- Sources.bibitems(target)) {
      println(s)
      for (a <- Citation(s._2).arxiv) {
        println("-- " + a.identifier + ": " + a.citation)
        if (!done.contains(a.identifier)) {
          targets += a.identifier
        }
      }
      println
    }
  }

  Slurp.quit
}