package net.tqft.mlp

import net.tqft.wiki.WikiMap
import net.tqft.mathscinet.Search
import net.tqft.journals.ISSNs
import net.tqft.mathscinet.Article
import net.tqft.eigenfactor.Eigenfactor
import net.tqft.util.pandoc
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.Date

object TOCBot extends App {

  //  Article.enableBibtexSaving
  while (true) {
    lazy val tocbot = {
      val b = WikiMap("http://tqft.net/mlp/index.php")
      b.login("tocbot", "zytopex")
      b.setThrottle(15000)
      // using .caching() causes thread problems...
//      import net.tqft.toolkit.collections.MapCaching._
//      b.caching()
      b
    }

    def summarize(articles: Iterator[Article]) = {
      var notClassified = 0
      var noneAvailable = 0
      var availableAtArxiv = 0
      var availableElsewhere = 0
      articles.map({ a => Thread.sleep(5000); tocbot.get("Data:" + a.identifierString + "/FreeURL") }).foreach({
        case Some(content) if content.contains("arxiv") => availableAtArxiv += 1
        case Some(content) if content.contains("http") => availableElsewhere += 1
        case None => notClassified += 1
        case _ => noneAvailable += 1
      })

      List(availableAtArxiv, availableElsewhere, notClassified, noneAvailable)
    }

    val arranged3 = extendedCoverage.toSeq.groupBy(_.journalOption)
    val arranged2 = arranged3.mapValues(_.groupBy(_.yearOption))
    val arranged = arranged2.mapValues(_.mapValues(_.groupBy(_.volumeYearAndIssue)))

    val yearSummaries = arranged2.mapValues(_.mapValues(s => summarize(s.iterator)))

    def journalText(journal: String) = ""

    def tokenizeIssue(issue: String): Seq[Either[String, Int]] = {
      import net.tqft.toolkit.Extractors.Int
      issue.split(" ").toSeq.map(_.stripPrefix("(").stripSuffix(")") match {
        case Int(i) => Right(i)
        case s => Left(s)
      })
    }

    import net.tqft.toolkit.collections.LexicographicOrdering._
    implicit val eitherOrdering = new Ordering[Either[String, Int]] {
      def compare(x: Either[String, Int], y: Either[String, Int]) = {
        (x, y) match {
          case (Left(xs), Left(ys)) => xs.compareTo(ys)
          case (Left(xs), Right(yi)) => -1
          case (Right(xi), Left(ys)) => 1
          case (Right(xi), Right(yi)) => xi - yi
        }
      }
    }

    for ((Some(journal), articles) <- arranged3) {
      tocbot("Data:" + journal + "/Progress") = summarize(articles.iterator).mkString("{{progress|", "|", "}}")
    }

    for ((Some(journal), years) <- arranged) {
      val j = pandoc.latexToText(journal)
      val text = journalText(journal) + (for ((Some(year), issues) <- years.toSeq.sortBy(y => y._1.map(0 - _))) yield {
        def s(issue: String) = {
          summarize(issues(issue).iterator).mkString("{{progress|", "|", "}}")
        }
        val yearSummary = yearSummaries(Some(journal))(Some(year))
        issues.keySet.toSeq.sortBy(tokenizeIssue).map(i => "* " + s(i) + " [[" + j + "/" + i + "]]").mkString("==" + year + " " + yearSummary.mkString("{{progress|", "|", "}}") + "==\n" + (now :: yearSummary.sum :: yearSummary).mkString("{{progress-text|", "|", "}}\n"), "\n", "\n")
      }).toSeq.mkString("\n")
      tocbot(j) = text

      for ((issue, articles) <- years.values.flatten) {
        tocbot(j + "/" + issue) = articles.map("{{toc|" + _.identifierString + "}}").sorted.mkString("Back to [[{{#titleparts:{{PAGENAME}}|1|}}]]\n\n{|\n", "\n", "\n|}")
      }
    }

    def now = {
      val f = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss")
      f.setTimeZone(TimeZone.getTimeZone("UTC"));
      f.format(new Date()) + " UTC";
    }

    println("All finished!")

    net.tqft.wiki.FirefoxDriver.quit

    Thread.sleep(1000 * 60 * 60 * 8)
  }
}