package net.tqft.kovacs

import net.tqft.toolkit.collections.Split._
import scala.io.Source
import net.tqft.mathscinet.Article

/**
 * @author scott
 */
object ParsePubs extends App {
  val lines = Source.fromFile("/Users/scott/Dropbox/LGK/Pubs.tex").getLines.toStream

  val refs = lines.splitOn(_.isEmpty).filter(_.head == "\\ref").map({ ref =>
    (ref.mkString("\n").split("\n\\\\").map(_.split("\\s").toList match {
      case tag :: remainder => tag.replaceAll("nofrills\\\\", "") -> remainder.mkString(" ").replaceAll("\n", " ").trim
      case Nil => ???
    })).toMap - "" - "\\ref" - "endref" - "nofrills"
  })

  for (ref <- refs) {
    val matches = net.tqft.citationsearch.Search.goodMatch(ref.get("paper").getOrElse(ref("book")) + " Kovacs " + ref.get("by").getOrElse("").replaceAll("with", "") + " " + ref.get("jour").getOrElse("") + " " + ref.get("year").getOrElse("")).map(_.citation)
    matches.map({ c => println(c.MRNumber) })
    matches.map({ c => println(c.arXiv) })
    matches.map({ c => c.MRNumber.map(i => println(Article(i).DOI)) })
  }
}