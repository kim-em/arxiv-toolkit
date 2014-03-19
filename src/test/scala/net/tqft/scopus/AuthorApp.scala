package net.tqft.scopus

import net.tqft.util.FirefoxSlurp

object AuthorApp extends App {

  val publications = Author(7201672329L).publications.toStream
  for (p <- publications) {
    println(p)
  }

  println(publications.head.dataText.mkString("\n"))
  println(publications.head.citationOption)

  FirefoxSlurp.quit
}