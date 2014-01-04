package net.tqft.mlp

import net.tqft.mathscinet.Article
import net.tqft.wiki.WikiMap
import net.tqft.eigenfactor.Eigenfactor
import net.tqft.mathscinet.Search

object SummaryBot extends App {

  lazy val bot = {
    val b = WikiMap("http://tqft.net/mlp/index.php")
    b.login("summarybot", "zytopex")
    b
  }

  def summarize(articles: Iterator[Article]) = {
    var notClassified = 0
    var noneAvailable = 0
    var availableAtArxiv = 0
    var availableElsewhere = 0
    articles.map(a => bot.get("Data:" + a.identifierString + "/FreeURL")).foreach({
      case None => notClassified += 1
      case Some("none available") => noneAvailable += 1
      case Some(content) if content.contains("arxiv") => availableAtArxiv += 1
      case _ => availableElsewhere += 1
    })

    List(notClassified, noneAvailable, availableAtArxiv, availableElsewhere)
  }

  def summaryText(articles: Iterator[Article]) = {
    val s = summarize(articles)
    "Of " + s.sum + " articles, " + s(2) + " are available on the arXiv, " +
      s(3) + " are available from other sources, and " + s(1) + " do not appear to be freely accessible. " +
      "(The remaining " + s(0) + " have not yet been classified.)"
  }

  val journals = Eigenfactor.topJournals.take(1)
  val years = 2013 to 2013

  def articles = for (j <- journals; y <- years; a <- Search.inJournalYear(j, y)) yield a

  val arranged = articles.toSeq.groupBy(_.journalOption).mapValues(_.groupBy(_.yearOption))

  for ((Some(journal), years) <- arranged; (Some(year), articles) <- years) {
    bot("Data:" + journal + "/YearSummary/" + year) = summaryText(articles.iterator)
  }

}