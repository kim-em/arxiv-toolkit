package net.tqft.util

import net.tqft.toolkit.amazon.AnonymousS3
import net.tqft.toolkit.Logging

case class BIBTEX(documentType: String, identifier: String, data: List[(String, String)]) {
  val map = data.map(p => (p._1.toUpperCase, p._2)).toMap
  def get(key: String) = map.get(key.toUpperCase)

  def toBIBTEXString = {
    "@" + documentType + " {" + identifier + ",\n" +
      data.map(p => ("          " + p._1).takeRight(10) + " = {" + p._2 + "},\n").mkString + "}\n"
  }

//  def save = BIBTEX.save(this)
}

object BIBTEX extends Logging {
  val cache = scala.collection.mutable.Map[String, String]()
//  lazy val cache = AnonymousS3("LoM-bibtex")
  lazy val DOI2mathscinet = AnonymousS3("DOI2mathscinet")

  
  
//  lazy val cachedKeys = {
////    ???
//    info("Fetching key set for LoM-bibtex")
//    val result = cache.keySet
//    info("   ... finished, found " + result.size + " keys")
//    result
//  }
//  private def save(item: BIBTEX) = {
////    if (!cachedKeys.contains(item.identifier)) {
//      info("Storing BIBTEX for " + item.identifier + " to S3")
//      cache.putIfAbsent(item.identifier, item.toBIBTEXString)
//      item.data.find(_._1 == "DOI").map({
//        case ("DOI", doi) =>
//          DOI2mathscinet.putIfAbsent(doi, item.identifier)
//      })
////    } else {
////      false
////    }
//  }

  // this is just parses mathscinet BIBTEX, which is particularly easy.
  def parse(bibtexString: String): Option[BIBTEX] = {
    val allLines = bibtexString.split("\n").toList
    def discardPreamble(lines: List[String]): List[String] = {
      lines match {
        case Nil => Nil
        case h :: t if h.trim.startsWith("@preamble") => discardPreamble(t.dropWhile(!_.trim.startsWith("@")))
        case lines => lines
      }
    }

    val lines = discardPreamble(allLines).filterNot(_.trim.startsWith("</") /* sometimes DOIs look like they contain a tag, and browsers sometimes insert spurious close tags when reporting the source */ )
    val DocumentTypePattern = """@([a-z ]*)\{[A-Za-z0-9]*,""".r
    val documentType = lines.head match {
      case DocumentTypePattern(t) => t.trim
    }
    val IdentifierPattern = """@[a-z ]*\{([A-Za-z0-9]*),""".r
    val identifier = lines.head match {
      case IdentifierPattern(id) => {
        // MathSciNet ids are sometimes padded with zeroes, sometimes not. We try to enforce this.
        if(id.startsWith("MR") && id.length < 9) {
          "MR" + ("0000000" + id.drop(2)).takeRight(7)
        } else {
          id
        }
      }
    }
    require(lines.last == "}", "Failed while parsing BIBTEX: " + bibtexString)
    val data = lines.tail.init.mkString("\n").replaceAll("\n             ", "").split("\n").map(line => line.replaceAllLiterally("&gt;", ">").replaceAllLiterally("&lt;", "<").replaceAllLiterally("&amp;", "&")).map(line => line.take(10).trim -> line.drop(14).dropRight(2)).toList

    Some(BIBTEX(documentType, identifier, data))

  }
}