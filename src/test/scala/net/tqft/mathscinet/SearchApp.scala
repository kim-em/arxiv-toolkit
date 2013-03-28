package net.tqft.mathscinet

import net.tqft.util.Throttle
import net.tqft.eigenfactor.Eigenfactor

object SearchApp extends App {
  // everything in Annals
  val inAnnals = Search.inJournal("0003-486X")
  def inTopJournals = {
    import net.tqft.toolkit.collections.FlexibleTranspose._
    Eigenfactor.topJournals.take(20).iterator.map(Search.inJournal).flexibleTranspose.flatMap({ x => x })
  }

//  for (article <- Search.by("Walker")) {
//    println(article.bibtex.toBIBTEXString)
//  }
  
//    for (article <- Search.everything) {
//      println(article.identifierString + " ---> " + article.DOI)
//    }

  for (article <- inTopJournals) {
    println(article.bibtex.toBIBTEXString)
  }
}

