package net.tqft.scopus

import net.tqft.util.FirefoxSlurp

object AuthorApp extends App {

  val publications = Author(7201672329L).publications.toStream
  for (p <- publications) {
    println(p)
  }

  for (p <- publications.headOption) {
//    println(p.dataText.mkString("\n"))
    println(p.citationOption)
    println(s"Cited ${p.numberOfCitations} times.")

    for (r <- p.matches) {
      println(r)
    }

    for ((r, m) <- p.referenceMatches) {
      println(r)
      if (m(0).score > m(1).score * 2) {
        println("   " + m(0).citation)
      }
    }
  }

  FirefoxSlurp.quit
}