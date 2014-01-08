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

object NewTOCBot extends App {

  //  Article.enableBibtexSaving

  lazy val tocbot = {
    val b = WikiMap("http://tqft.net/mlp/index.php")
    b.login("tocbot", "zytopex")
    b
  }

  val arranged2 = extendedCoverage.toSeq.groupBy(_.journalOption).mapValues(_.groupBy(_.yearOption))
  val arranged = arranged2.mapValues(_.mapValues(_.groupBy(_.volumeYearAndIssue)))

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

  for ((Some(journal), years) <- arranged) {
    val j = pandoc.latexToText(journal)
//    val text = (for ((Some(year), issues) <- years.toSeq.sortBy(y => y._1.map(0 - _))) yield {
//    }).toSeq.sorted.mkString("\n")
//    tocbot(j) = text

    for((Some(year), issues) <- years) {
      tocbot("Data:" + j + "/" + year + "/Contents") = issues.keySet.toSeq.sortBy(tokenizeIssue).mkString("/\n")
    }
    
    for ((issue, articles) <- years.values.flatten) {
      tocbot("Data:" + j + "/" + issue + "/Contents") = articles.map(_.identifierString).sorted.mkString("/")
      // TODO: maybe only write this next one if the page doesn't exist?
      tocbot(j + "/" + issue) = "Back to [[{{#titleparts:{{PAGENAME}}|1|}}]]\n{{GenerateTOC}}"
      tocbot("Data:" + j + "/" + issue + "/Progress") = "{{MeasureProgressFor|" + j + "/" + issue + "}}"
    }
  }

  net.tqft.wiki.FirefoxDriver.quit
}