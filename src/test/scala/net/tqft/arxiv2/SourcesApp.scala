package net.tqft.arxiv2

import net.tqft.citationsearch.CitationScore
import net.tqft.util.pandoc

object SourcesApp extends App {
  for ((key, text, o) <- Sources.referencesResolved("0705.0069")) {
    println(pandoc.latexToText(text))
    o.map({
      case CitationScore(citation, score) =>
        println(citation.best)
        println(citation.title + " - " + citation.authors + " - " + citation.citation_text)
    })
    println()
  }
}