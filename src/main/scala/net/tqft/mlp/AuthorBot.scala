package net.tqft.mlp

import net.tqft.mathscinet.Search
import net.tqft.journals.ISSNs
import net.tqft.wiki.WikiMap
import net.tqft.mathscinet.Article
import net.tqft.util.pandoc
import net.tqft.eigenfactor.Eigenfactor

object AuthorBot extends App {

  lazy val authorbot = {
    val b = WikiMap("http://tqft.net/mlp/index.php")
    b.login("authorbot", "zytopex")
    b.setThrottle(45000)
    b
  }

  //  Article.enableBibtexSaving

  for (a <- extendedCoverage ++ topJournals(100)) {
    println("posting authors for " + a.identifierString)
    authorbot("Data:" + a.identifierString + "/Authors") = a.authors.map(a => pandoc.latexToText(a.name)).mkString(" and ")
  }

  println("Done entering authors!")
  
  net.tqft.wiki.FirefoxDriver.quit
}