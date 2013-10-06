package net.tqft.mathscinet

import java.util.Calendar
import net.tqft.toolkit.amazon.S3

case class Author(id: Int, name: String) {
  def lastName = name.takeWhile(c => c != ',' && c != ' ')
  def articles = Search.query("pg1" -> "IID", "s1" -> id.toString)
  def hIndex(years: Int = 200) = Author.hIndex(id, years)
}

object Author {
  val hIndexCache = {
    import net.tqft.toolkit.collections.MapTransformer._
    S3("hIndex").transformKeys({ k: String => k.toInt }, { k: Int => k.toString })
      .transformValues({ v: String => v.toInt }, { v: Int => v.toString })
  }

  def hIndex(id: Int, years: Int) = {
    if(years == 200) {
      hIndexCache.getOrElseUpdate(id, hIndexImplementation(id, 200))
    } else {
      hIndexImplementation(id, years)
    }
  }
  
  private def hIndexImplementation(id: Int, years: Int = 200) = {
    val firstYear = Calendar.getInstance().get(Calendar.YEAR) - years
    val citations = Author(id, "").articles.filter(article => article.yearOption.nonEmpty && article.year > firstYear).map(_.numberOfCitations).toList.sorted.reverse
    println("citations: " + citations)

    (0 +: citations).zipWithIndex.tail
      .takeWhile(p => p._1 >= p._2).lastOption.map(p => p._2).getOrElse(0)
  }

}