package net.tqft.mlp

import net.tqft.mathscinet.Search
import net.tqft.journals.ISSNs
import net.tqft.wiki.WikiMap
import net.tqft.mathscinet.Article
import net.tqft.util.pandoc
import net.tqft.eigenfactor.Eigenfactor

object TitleBot extends App {

  lazy val titlebot = {
    val b = WikiMap("http://tqft.net/mlp/index.php")
    b.login("titlebot", "zytopex")
    b.setThrottle(45000)
    b
  }

  //  Article.enableBibtexSaving

  for (a <-extendedCoverage ++ topJournals(100)) {
    println("posting title for " + a.identifierString)
    titlebot("Data:" + a.identifierString + "/Title") = a.textTitle
  }

  println("Done entering titles!")

  net.tqft.wiki.FirefoxDriver.quit
}