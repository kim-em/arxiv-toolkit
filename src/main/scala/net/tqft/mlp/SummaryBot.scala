package net.tqft.mlp

import net.tqft.mathscinet.Article
import net.tqft.wiki.WikiMap
import net.tqft.eigenfactor.Eigenfactor
import net.tqft.mathscinet.Search
import net.tqft.journals.ISSNs
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.Date

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
      case Some(content) if content.contains("arxiv") => availableAtArxiv += 1
      case Some(content) if content.contains("http") => availableElsewhere += 1
      case _ => noneAvailable += 1
    })

    List(availableAtArxiv, availableElsewhere, notClassified, noneAvailable)
  }

  def summaryText(articles: Iterator[Article]) = {
    def now = {
      val f = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss")
      f.setTimeZone(TimeZone.getTimeZone("UTC"));
      f.format(new Date()) + " UTC";
    }

    val s = summarize(articles)
      (now :: s.sum :: s).mkString("{{progress-text|", "|" , "}}")
  }

//    val journals = Eigenfactor.topJournals.take(1)
  val years = 2013 to 2013

  val arranged = currentCoverage.toSeq.groupBy(_.journalOption).mapValues(_.groupBy(_.yearOption))

  for ((Some(journal), years) <- arranged; (Some(year), articles) <- years) {
    bot("Data:" + journal + "/YearSummary/" + year) = summaryText(articles.iterator)
  }

  net.tqft.wiki.FirefoxDriver.quit
}