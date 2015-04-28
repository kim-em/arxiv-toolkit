package net.tqft.scopus

object MatchCitationsAgainstWebOfScienceApp extends App {

  val article = Article(args(0))

  val (matches, unmatchedScopus, unmatchedWebOfScience) = MatchCitationsAgainstWebOfScience(article)
  println("Found the following matching pairs:")
  println("---")
  for ((scopusArticle, webOfScienceCitation) <- matches) {
    println(scopusArticle.fullCitation)
    println(webOfScienceCitation.fullCitation)
    println("---")
  }
  println
  println("Found the following citations on Scopus, which do not appear on Web of Science:")
  println("---")
  for (article <- unmatchedScopus) {
    println(article.fullCitation)
    println("---")
  }
  println("Found the following citations on Web of Science, which do not appear on Scopus:")
  println("---")
  for (article <- unmatchedWebOfScience) {
    println(article.fullCitation)
    println("---")
  }

  net.tqft.scholar.FirefoxDriver.quit
  net.tqft.util.FirefoxSlurp.quit
}