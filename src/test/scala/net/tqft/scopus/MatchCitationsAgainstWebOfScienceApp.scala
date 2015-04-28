package net.tqft.scopus

object MatchCitationsAgainstWebOfScienceApp extends App {

  val articles = if (args(0).startsWith("2-s2.0-")) {
    Seq(Article(args(0)))
  } else {
    val name = args.tail.mkString(" ")
    println(s"Considering all articles by author '$name', with Scopus identifier ${args(0)}.")
    Author(args(0).toLong, name).publications
  }

  for (article <- articles) {
    println("Considering citations for " + article.fullCitation)

    if (article.onWebOfScience.isEmpty) {
      println("No matching article found on Web Of Science!")
    } else {
      val (matches, unmatchedScopus, unmatchedWebOfScience) = MatchCitationsAgainstWebOfScience(article)
      println("Found the following matching pairs:")
      println("---")
      for ((scopusArticle, webOfScienceCitation) <- matches) {
        println(scopusArticle.fullCitation)
        println(webOfScienceCitation.fullCitation)
        println("---")
      }
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
    }
    
    println
  }

  net.tqft.scholar.FirefoxDriver.quit
  net.tqft.util.FirefoxSlurp.quit

}