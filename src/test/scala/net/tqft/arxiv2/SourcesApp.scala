package net.tqft.arxiv2

import net.tqft.citationsearch.CitationScore
import net.tqft.util.pandoc

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
  for(s <- Sources.bibitems("1701.00567")) {
    println(s)
    println
  }
}