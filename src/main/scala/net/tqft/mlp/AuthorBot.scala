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
    b
  }

  //  Article.enableBibtexSaving

  val journals = selectedJournals //++ Eigenfactor.topJournals.take(100)
  val years = 2013 to 2013

  def articles = for (j <- journals; y <- years; a <- Search.inJournalYear(j, y)) yield a

  for (a <- articles) {
    println("posting authors for " + a.identifierString)
    authorbot("Data:" + a.identifierString + "/Authors") = a.authors.map(a => pandoc.latexToText(a.name)).mkString(" and ")
  }

  net.tqft.wiki.FirefoxDriver.quit
}