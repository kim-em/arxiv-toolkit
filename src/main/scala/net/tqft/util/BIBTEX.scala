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
  
  def save = BIBTEX.save(this)
}

object BIBTEX extends Logging {
  lazy val cache = AnonymousS3("LoM-bibtex")
  lazy val cachedKeys = {
    info("Fetching key set for LoM-bibtex")
    val result = cache.keySet
    info("   ... finished, found " + result.size + " keys")
    result
  }
  private def save(item: BIBTEX) = {
    if (!cachedKeys.contains(item.identifier)) {
      info("Storing BIBTEX for " + item.identifier + " to S3")
      cache.putIfAbsent(item.identifier, item.toBIBTEXString)
    } else {
      false
    }
  }

// this is just parses mathscinet BIBTEX, which is particularly easy.
  def parse(bibtexString: String): Option[BIBTEX] = {
    if (bibtexString.startsWith("@preamble")) {
      None
    } else {
      val lines = bibtexString.split("\n")
      val DocumentTypePattern = """@([a-z ]*)\{[A-Za-z0-9]*,""".r
      val documentType = lines.head match {
        case DocumentTypePattern(t) => t.trim
      }
      val IdentifierPattern = """@[a-z ]*\{([A-Za-z0-9]*),""".r
      val identifier = lines.head match {
        case IdentifierPattern(id) => id
      }
      require(lines.last == "}")
      val data = lines.tail.init.mkString("\n").replaceAll("\n             ", "").split("\n").map(line => line.take(10).trim -> line.drop(14).dropRight(2)).toList

      Some(BIBTEX(documentType, identifier, data))
    }
  }
}