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
        case Seq(one, suffix @ ("Jr." | "Jr" | "Sr." | "jr." | "jun." | """J{\'u}nior""" | "Junior" | "ml." | "I" | "II" | "II." | "III" | "III." | "3rd" | "3rd." | "IV" | "V" | "V." | "S.J." | "S. J." | "E. G." | "M.a."), two) => two + " " + one + ", " + suffix
        case Seq("Vogan", "David A.", "Jr.") => "David A. Vogan, Jr."
        case Seq("Miller", "G.", "V") => "G. Miller, V"
        case Seq("Day", "M.", "Mahlon") => "Mahlon M. Day"
        case Seq("Arnold", "James", "Jr.", "E.") => "James E. Arnold, Jr."
        case Seq("Rodeja", "E. G.-", "F.") => "F. Rodeja, E. G."
        case Seq("Cooper", "Randolph G.", "III", "Jr.") => "Randolph G. Cooper, III, Jr." // That doesn't even make sense.
        case Seq("Young", "Gail", "Jr.", "S") => "Gail S. Young, Jr."
        case Seq("Young", "Gail", "Jr.", "S.") => "Gail S. Young, Jr."
        case Seq("Good", "J.", "I") => "J. Good, I"
        case Seq("Ramamohana", "C.", "Rao") => "Rao C. Ramamohana"
        case Seq("Dou", "Alberto", "S. I.") => "Alberto Dou, S. I."
        case Seq("Kolos", "C. C. J.", "W. Roothaan") => "W. Roothaan Kolos, C. C. J."
        case Seq("Snyder", "M.", "Willard") => "Willard M. Snyder"
        case Seq("Long", "E.", "Paul") => "Paul E. Long"
        case Seq("Best", "J.", "Michael") => "Michael J. Best"
        case Seq("Kou", "R.", "Shauying") => "Shauying R. Kou"
        case Seq("Besieris", "M.", "Ioannis") => "Ioannis M. Besieris"
        case Seq("Kuiper", "H.", "Nicolaas") => "Nicolaas H. Kuiper"
        case Seq("da Rocha", "Galdino", "Filho") => "Galdino da Rocha, Filho"
        case Seq("Ditor", "Z.", "Seymour") => "Seymour Z. Ditor"
        case Seq("Sankara", "K.", "Pillai") => "Pillai K. Sankara"
        case Seq("Kazarjan", "A.", """{\`E}""") => """{\`E}. A. Kazarjan"""
        case Seq("Cruz", "A.", "Marianito") => "Marianito A. Cruz"
        case Seq("Keezer", "Sister", "James Miriam") => "James Miriam Keezer"  
        case Seq("Wolf", "R.", "Thomas") => "Thomas R. Wolf"
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