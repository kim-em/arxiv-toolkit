package net.tqft.mathscinet

import net.tqft.util.Slurp
import net.tqft.toolkit.collections.Split._
import net.tqft.toolkit.collections.TakeToFirst._
import java.util.Calendar
import scala.util.Random
import net.tqft.eigenfactor.Eigenfactor

object Search {
  def query(q: String): Iterator[Article] = {
    def queryString(k: Int) = "http://www.ams.org/mathscinet/search/publications.html?" + q + "&r=" + (1 + 100 * k).toString + "&extend=1&fmt=bibtex"
    def queries = Iterator.from(0).map(queryString).map(Slurp.attempt).takeWhile(_.isLeft).map(_.left.get).flatten

    def truncatedQueries = {
      val smallMatches = """<strong>Matches:</strong> [0-9]{1,2}$""".r

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
  def inJournal(text: String) = query("pg4" -> "JOUR", "s4" -> text)
  def during(k: Int) = query("arg3" -> k.toString, "dr" -> "pubyear", "pg8" -> "ET", "yrop" -> "eq")
  def between(start: Int, end: Int) = query("yearRangeFirst" -> start.toString, "yearRangeSecond" -> end.toString, "dr" -> "pubyear", "pg8" -> "ET", "yrop" -> "eq")

  private def currentYear = Calendar.getInstance().get(Calendar.YEAR)
  private def allYears = (currentYear to 1810 by -1)

  def inJournalsJumbled(strings: Seq[String]) = {
    val yearMaps1 = for (k <- (currentYear to 1980 by -1)) yield Map("arg3" -> k.toString, "dr" -> "pubyear", "pg8" -> "ET", "yrop" -> "eq")
    val yearMaps2 = for (k <- (1970 to 1940 by -10)) yield Map("yearRangeFirst" -> k.toString, "yearRangeSecond" -> (k + 9).toString, "dr" -> "pubyear", "pg8" -> "ET", "yrop" -> "eq")
    val yearMaps3 = Seq(Map("yearRangeFirst" -> "1810", "yearRangeSecond" -> "1939", "dr" -> "pubyear", "pg8" -> "ET", "yrop" -> "eq"))
    Random.shuffle(for (y <- yearMaps1 ++ yearMaps2 ++ yearMaps3; s <- strings) yield query(y ++ Map("pg4" -> "JOUR", "s4" -> s))).iterator.flatten
  }
  def inTopJournalsJumbled(number: Int = 20) = {
    inJournalsJumbled(Eigenfactor.topJournals.take(number))
  }

  def everything = allYears.iterator.flatMap(during)

}