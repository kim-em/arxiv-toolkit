package net.tqft.mathscinet

import net.tqft.util.Slurp
import net.tqft.toolkit.collections.Split._
import net.tqft.toolkit.collections.TakeToFirst._
import java.util.Calendar
import scala.util.Random
import net.tqft.eigenfactor.Eigenfactor
import java.util.Date
import net.tqft.toolkit.Logging

object Search extends Logging {
  def query(q: String): Iterator[Article] = {
    def queryString(k: Int) = "http://www.ams.org/mathscinet/search/publications.html?" + q + "&r=" + (1 + 100 * k).toString + "&extend=1&fmt=bibtex"
    def queries = Iterator.from(0).map(queryString).map(Slurp.attempt).takeWhile(_.isLeft).map(_.left.get).flatten

    def truncatedQueries = {
      val smallMatches = """<strong>Matches:</strong> ([0-9]{1,2}|100)$""".r

      queries
        .takeWhile(_ != "<title>500 Internal Server Error</title>")
        .takeWhile(!_.startsWith("No publications results"))
        .takeWhile(_ != """<span class="disabled">Next</span>""")
        .takeWhile(smallMatches.findAllIn(_).isEmpty)
    }

    def bibtexChunks = truncatedQueries.splitBefore(line => line.contains("<pre>")).filter(lines => lines.head.contains("<pre>")).map(lines => lines.iterator.takeToFirst(line => line.contains("</pre>")).mkString("\n").trim.stripPrefix("<pre>").stripSuffix("</pre>").trim)

    bibtexChunks.grouped(99).flatMap(group => group.par.flatMap(Article.fromBibtex))
  }

  val defaultQuery = Map[String, String](
    "pg4" -> "AUCN",
    "s4" -> "",
    "co4" -> "AND",
    "pg5" -> "TI",
    "s5" -> "",
    "co5" -> "AND",
    "pg6" -> "PC",
    "s6" -> "",
    "co6" -> "AND",
    "pg7" -> "ALLF",
    "s7" -> "",
    "co7" -> "AND",
    "dr" -> "all",
    "yrop" -> "eq",
    "arg3" -> "",
    "yearRangeFirst" -> "",
    "yearRangeSecond" -> "",
    "pg8" -> "ET",
    "s8" -> "All")
  val defaultParameters = List("pg4", "s4", "co4", "pg5", "s5", "co5", "pg6", "s6", "co6", "pg7", "s7", "co7", "dr", "yrop", "arg3", "yearRangeFirst", "yearRangeSecond", "pg8", "s8")

  def query(qs: (String, String)*): Iterator[Article] = query(qs.toMap)
  def query(q: Map[String, String]): Iterator[Article] = {
    val compositeQuery = defaultQuery ++ q
    val compositeParameters = (defaultParameters ++ q.keys).distinct
    query((for (p <- compositeParameters) yield p + "=" + compositeQuery(p)).mkString("&"))
  }

  def by(author: String) = query("pg4" -> "AUCN", "s4" -> author)
  def inJournal(text: String) = inJournalsJumbled(Seq(text))
  def during(k: Int) = query("arg3" -> k.toString, "dr" -> "pubyear", "pg8" -> "ET", "yrop" -> "eq")
  def between(start: Int, end: Int) = query("yearRangeFirst" -> start.toString, "yearRangeSecond" -> end.toString, "dr" -> "pubyear", "pg8" -> "ET", "yrop" -> "eq")

  private def currentYear = Calendar.getInstance().get(Calendar.YEAR)
  private def allYears = (currentYear to 1810 by -1)

  def inJournalYear(text: String, year: Int) = query("pg4" -> "JOUR", "s4" -> text, "arg3" -> year.toString, "dr" -> "pubyear", "pg8" -> "ET", "yrop" -> "eq")
  def inJournalBetween(text: String, start: Int, end: Int) = query("pg4" -> "JOUR", "s4" -> text, "yearRangeFirst" -> start.toString, "yearRangeSecond" -> end.toString, "dr" -> "pubyear", "pg8" -> "ET", "yrop" -> "eq")
  
  def inJournalsJumbled(strings: Iterable[String]) = {
    val years = currentYear to 1980 by -1
    val ranges = (1810,1939) +: (1970 to 1940 by -10).map(y => (y, y+9)) 
        
    val searches = Random.shuffle((for(year <- years; s <- strings) yield inJournalYear(s, year)) ++ (for((start, end) <- ranges; s <- strings) yield inJournalBetween(s, start, end)))
    
    val totalSearches = searches.size
    val beginTime = new Date().getTime
    var completedSearches = 0
    def recordProgress(i: Iterator[Article]) = {
      completedSearches += 1
      info("Beginning search " + completedSearches + " of " + totalSearches + (if(completedSearches > 1) (", estimated time remaining = " + (totalSearches - completedSearches - 1 + 0.0)/(completedSearches - 1) * (new Date().getTime - beginTime) / 1000) else ""))
      i
    }
    
    searches.iterator.map(recordProgress).flatten
  }
  def inTopJournalsJumbled(number: Int = 20) = {
    info("Beginning search over the top " + number + " journals according to 'article influence' at Eigenfactor.")
    
    inJournalsJumbled(Eigenfactor.topJournals.take(number))
  }

  def everything = allYears.iterator.flatMap(during)

}