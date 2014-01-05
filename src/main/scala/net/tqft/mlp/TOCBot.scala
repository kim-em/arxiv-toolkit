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
//  val journals = Iterator(ISSNs.`Advances in Mathematics`, ISSNs.`Discrete Mathematics`, ISSNs.`Annals of Mathematics`, ISSNs.`Algebraic & Geometric Topology`, ISSNs.`Geometric and Functional Analysis`)
  val years = 2013 to 2013

  val articles = for (j <- journals; y <- years; a <- Search.inJournalYear(j, y)) yield a

  val arranged = articles.toSeq.groupBy(_.journalOption).mapValues(_.groupBy(_.yearOption)).mapValues(_.mapValues(_.groupBy(_.volumeYearAndIssue)))

  def journalText(journal: String) = ""
  def yearText(journal: String, year: Int) = "{{#ifexist:Data:" + journal + "/YearSummary/" + year + "|{{Data:" + journal + "/YearSummary/" + year + "}}}}\n\n"

  for ((Some(journal), years) <- arranged) {
    val j = pandoc.latexToText(journal)
    val text = journalText(journal) + (for ((Some(year), issues) <- years) yield {
      issues.keySet.toSeq.sorted.map("* [[" + j + "/" + _ + "]]").mkString("==" + year + "==\n" + yearText(journal, year), "\n", "\n")
    }).toSeq.sorted.mkString("\n")
    tocbot(j) = text

    for ((issue, articles) <- years.values.flatten) {
      tocbot(j + "/" + issue) = articles.map("{{toc|" + _.identifierString + "}}").sorted.mkString("Back to [[{{#titleparts:{{PAGENAME}}|1|}}]]\n\n{|\n", "\n", "\n|}")
    }
  }

  net.tqft.wiki.FirefoxDriver.quit
}