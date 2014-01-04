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
  lazy val authorbot = {
    val b = WikiMap("http://tqft.net/mlp/index.php")
    b.login("authorbot", "zytopex")
    b
  }
  lazy val doibot = {
    val b = WikiMap("http://tqft.net/mlp/index.php")
    b.login("doibot", "zytopex")
    b
  }

  //  Article.enableBibtexSaving

//  def advancesArticles = for (a <- Search.inJournal(ISSNs.`Advances in Mathematics`); if a.journal != "Advancement in Math."; y <- a.yearOption; if y >= 2013) yield a
//  def annalsArticles = for (a <- Search.inJournal(ISSNs.`Annals of Mathematics`); y <- a.yearOption; if y >= 2013) yield a
//  def discreteMathArticles = for (a <- Search.inJournal(ISSNs.`Discrete Mathematics`); y <- a.yearOption; if y >= 2013) yield a
//  def agtArticles = for (a <- Search.inJournal(ISSNs.`Algebraic & Geometric Topology`); y <- a.yearOption; if y >= 2013) yield a
//  def gafaArticles = for (a <- Search.inJournal(ISSNs.`Geometric and Functional Analysis`); y <- a.yearOption; if y >= 2013) yield a
//
//  val articles = (advancesArticles ++ annalsArticles ++ discreteMathArticles ++ agtArticles ++ gafaArticles).toStream

  val journals = Eigenfactor.topJournals.take(100)
  val years = 2013 to 2013

  def articles = for (j <- journals; y <- years; a <- Search.inJournalYear(j, y)) yield a

  for (a <- articles) {
    println("posting authors for " + a.identifierString)
    authorbot("Data:" + a.identifierString + "/Authors") = a.authors.map(a => pandoc.latexToText(a.name)).mkString(" and ")
  }
  for (a <- articles) {
    for(link <- a.DOI.map("http://dx.doi.org/" + _).orElse(a.URL)) {
      println("posting link for " + a.identifierString)
      doibot("Data:" + a.identifierString + "/PublishedURL") = link

    }
  }
  for (a <- articles) {
    println("posting title for " + a.identifierString)
    titlebot("Data:" + a.identifierString + "/Title") = a.textTitle
  }

}