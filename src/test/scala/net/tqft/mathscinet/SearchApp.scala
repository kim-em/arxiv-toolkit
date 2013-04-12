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
  
  // AMS
   for (article <- Search.inJournalsJumbled(Seq("0002-9947", "1056-3911", "1073-7928", "0894-0347", "0075-4102", "1088-4173", "0273-0979", "0002-9939", "0025-5718", "0024-6107", "0002-9904", "0234-0852", "1088-4165", "0065-9266"))) {
    println(article.bibtex.toBIBTEXString)
  }
 
  
  for (article <- Search.inTopJournalsJumbled(100)) {
    println(article.bibtex.toBIBTEXString)
  }
  
  FirefoxSlurp.quit
}

