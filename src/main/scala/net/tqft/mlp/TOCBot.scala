package net.tqft.mlp

import net.tqft.wiki.WikiMap
import net.tqft.mathscinet.Search
import net.tqft.journals.ISSNs
import net.tqft.mathscinet.Article
import net.tqft.eigenfactor.Eigenfactor
import net.tqft.util.pandoc

object TOCBot extends App {
  
//  Article.enableBibtexSaving
  
  lazy val tocbot = {
    val b = WikiMap("http://tqft.net/mlp/index.php")
    b.login("tocbot", "zytopex")
    b
  }

  val journals = Eigenfactor.topJournals.take(100)
  val years = 2009 to 2012
  
  val articles = for(j <- journals; y <- years; a <- Search.inJournalYear(j, y)) yield a
  
//  def advancesArticles = for (a <- Search.inJournalYear(ISSNs.`Advances in Mathematics`, 2013); if a.journal != "Advancement in Math."; y <- a.yearOption; if y >= 2013) yield a
//  def annalsArticles = Search.inJournalYear(ISSNs.`Annals of Mathematics`, 2013)
//  def discreteMathArticles = Search.inJournalYear(ISSNs.`Discrete Mathematics`,2013)
//  def agtArticles = Search.inJournalYear(ISSNs.`Algebraic & Geometric Topology`, 2013)
//  def gafaArticles = Search.inJournalYear(ISSNs.`Geometric and Functional Analysis`,2013)
//
//  val articles = advancesArticles ++ annalsArticles ++ discreteMathArticles ++ agtArticles ++ gafaArticles

  val arranged = articles.toSeq.groupBy(_.journalOption).mapValues(_.groupBy(_.volumeYearAndIssue))

  for ((Some(journal), issues) <- arranged) {
    val j = pandoc.latexToText(journal)
    tocbot(j) = issues.keySet.toSeq.sorted.map("* [[" + j + "/" + _ + "]]").mkString("\n")
    for ((issue, articles) <- issues) {
      tocbot(j + "/" + issue) = articles.map("{{toc|" + _.identifierString + "}}").sorted.mkString("{|\n", "\n", "\n|}")
    }
  }

}