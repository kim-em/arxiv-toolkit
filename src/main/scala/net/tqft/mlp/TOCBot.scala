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

  def summarize(articles: Iterator[Article]) = {
    var notClassified = 0
    var noneAvailable = 0
    var availableAtArxiv = 0
    var availableElsewhere = 0
    articles.map(a => tocbot.get("Data:" + a.identifierString + "/FreeURL")).foreach({
      case None => notClassified += 1
      case Some(content) if content.contains("arxiv") => availableAtArxiv += 1
      case Some(content) if content.contains("http") => availableElsewhere += 1
      case _ => noneAvailable += 1
    })

    List(availableAtArxiv, availableElsewhere, notClassified, noneAvailable)
  }

  val arranged = currentCoverage.toSeq.groupBy(_.journalOption).mapValues(_.groupBy(_.yearOption)).mapValues(_.mapValues(_.groupBy(_.volumeYearAndIssue)))

  def journalText(journal: String) = ""
  def yearText(journal: String, year: Int) = "{{#ifexist:Data:" + journal + "/YearSummary/" + year + "|{{Data:" + journal + "/YearSummary/" + year + "}}}}\n\n"

  for ((Some(journal), years) <- arranged) {
    val j = pandoc.latexToText(journal)
    val text = journalText(journal) + (for ((Some(year), issues) <- years) yield {
      def s(issue: String) = {
        summarize(issues(issue).iterator).mkString("{{progress|", "|", "}}")
      }
      issues.keySet.toSeq.sorted.map(i => "* " + s(i) + " [[" + j + "/" + i + "]]").mkString("==" + year + "==\n" + yearText(journal, year), "\n", "\n")
    }).toSeq.sorted.mkString("\n")
    tocbot(j) = text

    for ((issue, articles) <- years.values.flatten) {
      tocbot(j + "/" + issue) = articles.map("{{toc|" + _.identifierString + "}}").sorted.mkString("Back to [[{{#titleparts:{{PAGENAME}}|1|}}]]\n\n{|\n", "\n", "\n|}")
    }
  }

  net.tqft.wiki.FirefoxDriver.quit
}