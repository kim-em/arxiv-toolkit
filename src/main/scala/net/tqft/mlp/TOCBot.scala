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
import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.Wiki
import net.tqft.util.FirefoxSlurp

object TOCBot extends App {

  FirefoxSlurp.disable

  lazy val tocbot = {
    val b = WikiMap("http://tqft.net/mlp/index.php")
    b.login("tocbot", "zytopex")
    b.enableSQLReads("jdbc:mysql://mysql.tqft.net/mathematicsliteratureproject?user=readonly1&password=readonly", "mlp_")
    b.setThrottle(1500)
    b
  }
  import scala.slick.driver.MySQLDriver.simple._

  val texts = SQL { implicit session =>
    val query = (for (
      p <- Wiki.Pages;
      if p.page_namespace === 100;
      if p.page_title like "%/FreeURL";
      r <- Wiki.Revisions;
      if r.rev_id === p.page_latest;
      t <- Wiki.Texts;
      if t.old_id === r.rev_text_id
    ) yield (p.page_title, t.old_text))

    println("Loading all /FreeURL pages from the wiki...")
    println(query.selectStatement)
    val result = query.list.map({ p => (p._1.dropRight(8), p._2) }).toMap
    println(" ... done")
    println(result.take(10))
    result
  }

  def summarize(articles: Iterator[Article]) = {
    var notClassified = 0
    var noneAvailable = 0
    var availableAtArxiv = 0
    var availableElsewhere = 0
    articles.map({ a => texts.get(a.identifierString) }).foreach({
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
    println("writing progress data for " + journal)
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
    println("writing journal page: " + j)
    tocbot(j) = text

    for ((issue, articles) <- years.values.flatten) {
      tocbot(j + "/" + issue) = articles.map("{{toc|" + _.identifierString + "}}").sorted.mkString("Back to [[{{#titleparts:{{PAGENAME}}|1|}}]]\n\n{|\n", "\n", "\n|}")
      println("writing issue page: " + j + "/" + issue)
    }
  }

  def now = {
    val f = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss")
    f.setTimeZone(TimeZone.getTimeZone("UTC"));
    f.format(new Date()) + " UTC";
  }

  println("All finished!")

  net.tqft.wiki.FirefoxDriver.quit

}