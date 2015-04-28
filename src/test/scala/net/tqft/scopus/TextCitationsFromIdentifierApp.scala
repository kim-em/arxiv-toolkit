package net.tqft.scopus

import net.tqft.scholar.Scholar

object TextCitationsFromIdentifierApp extends App {
  val article = Article(args(0))
  for (citation <- article.citations) {
    val c = citation.title + ", " + citation.authorData
    println(c)
  }

  net.tqft.scholar.FirefoxDriver.quit
  net.tqft.util.FirefoxSlurp.quit
}