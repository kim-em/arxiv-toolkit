package net.tqft.mathscinet

import net.tqft.util.Throttle
import net.tqft.eigenfactor.Eigenfactor
import net.tqft.util.FirefoxSlurp

object SearchApp extends App {
  // everything in Annals
  val inAnnals = Search.inJournal("0003-486X")
  def inTopJournals = {
    import net.tqft.toolkit.collections.FlexibleTranspose._
    Eigenfactor.topJournals.take(20).iterator.map(Search.inJournal).flexibleTranspose.flatMap({ x => x })
  }

//  for (article <- Search.by("Izumi")) {
//    println(article.bibtex.toBIBTEXString)
//  }

  //    for (article <- Search.everything) {
  //      println(article.identifierString + " ---> " + article.DOI)
  //    }

  //  for (article <- inTopJournals) {
  //    println(article.bibtex.toBIBTEXString)
  //  }

//  for(article <- Search.inJournalYear("0003-486X", 1999)) {
//     println(article.bibtex.toBIBTEXString)   
//  }
//  for(article <- Search.inJournal("0003-486X")) {
//     println(article.bibtex.toBIBTEXString)   
//  }
  
  for (article <- Search.inTopJournalsJumbled(50)) {
    println(article.bibtex.toBIBTEXString)
  }
  
  FirefoxSlurp.quit
}

