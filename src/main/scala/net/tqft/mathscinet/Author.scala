package net.tqft.mathscinet

import java.util.Calendar
import net.tqft.toolkit.amazon.S3

case class Author(id: Int, name: String) {
  def lastName = name.takeWhile(c => c != ',' && c != ' ')
  def articles = Search.query("pg1" -> "IID", "s1" -> id.toString)
  def hIndex(firstYear: Int = 2008) = Author.hIndex(id, firstYear)
  
  def firstNameLastName = {
      name.split(",").map(_.trim).filter(_.nonEmpty).toSeq match {
        case Seq(one) => one
        case Seq(one, two) => two + " " + one
        case Seq(one, suffix @ ("Jr." | "Jr" | "Sr." | "jr." | "jun." | "I" | "II" | "III" | "IV" | "V"), two) => two + " " + one + ", " + suffix
        case Seq("Cooper", "Randolph G.", "III", "Jr.") => "Randolph G. Cooper, III, Jr." // That doesn't even make sense. 
      }
    }
}

object Author {
  val hIndexCache1996 = {
    import net.tqft.toolkit.collections.MapTransformer._
    S3("hIndex1996").transformKeys({ k: String => k.toInt }, { k: Int => k.toString })
      .transformValues({ v: String => v.toInt }, { v: Int => v.toString })
  }
val hIndexCache2008 = {
    import net.tqft.toolkit.collections.MapTransformer._
    S3("hIndex2008").transformKeys({ k: String => k.toInt }, { k: Int => k.toString })
      .transformValues({ v: String => v.toInt }, { v: Int => v.toString })
  }

  def hIndex(id: Int, firstYear: Int) = {
    if(firstYear == 1996) {
      hIndexCache1996.getOrElseUpdate(id, hIndexImplementation(id, firstYear))
    } else if(firstYear == 2008) {
      hIndexCache2008.getOrElseUpdate(id, hIndexImplementation(id, firstYear))
    } else {
      hIndexImplementation(id, firstYear)
    }
  }
  
  private def hIndexImplementation(id: Int, firstYear: Int) = {
    val citations = Author(id, "").articles.filter(article => article.yearOption.nonEmpty && article.year >= firstYear).map(_.numberOfCitations).toList.sorted.reverse
    println("citations: " + citations)

    (0 +: citations).zipWithIndex.tail
      .takeWhile(p => p._1 >= p._2).lastOption.map(p => p._2).getOrElse(0)
  }

}