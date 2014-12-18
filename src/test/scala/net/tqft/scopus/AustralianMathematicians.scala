package net.tqft.scopus

import net.tqft.util.FirefoxSlurp

object AustralianMathematicians extends App {

  val article=Article("2-s2.0-84908567331")
//  println(article.authorData)
//  println(article.authorAffiliations)
  for((author, affiliations) <- article.authorAffiliations) {
    println(author)
    for(affiliation <- affiliations) {
      println("   " + affiliation)
    }
  }
  
  FirefoxSlurp.quit
  
}