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
    b
  }

  //  Article.enableBibtexSaving

  val journals = Eigenfactor.topJournals.take(100)
  val years = 2013 to 2013

  def articles = for (j <- journals; y <- years; a <- Search.inJournalYear(j, y)) yield a

  for (a <- articles) {
    println("posting title for " + a.identifierString)
    titlebot("Data:" + a.identifierString + "/Title") = a.textTitle
  }

  net.tqft.wiki.FirefoxDriver.quit
}