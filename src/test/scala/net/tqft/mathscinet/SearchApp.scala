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

  //  for(article <- Search.inJournal("0003-486X")) {
  //     println(article.bibtex.toBIBTEXString)   
  //  }

  // Crelle's journal
  for (article <- Search.inJournal("0075-4102")) {
    println(article.bibtex.toBIBTEXString)
  }

  // IMRN
  for (article <- Search.inJournal("1073-7928")) {
    println(article.bibtex.toBIBTEXString)
  }

  // Bull. Amer. Math. Soc.
  for (article <- Search.inJournal("0002-9904")) {
    println(article.bibtex.toBIBTEXString)
  }

  // AMS
  for (article <- Search.inJournalsJumbled(Seq("0002-9947", "1056-3911", "1073-7928", "0894-0347", "0075-4102", "1088-4173", "0273-0979", "0002-9939", "0025-5718", "0024-6107", "0002-9904", "0234-0852", "1088-4165", "0065-9266"))) {
    println(article.bibtex.toBIBTEXString)
  }

  // Physical Review
  for (article <- Search.inJournalsJumbled(Seq("0034-6861", "1050-2947", "0163-1829", "0556-2813", "1550-7998", "0556-2821", "1539-3755", "0031-9007", "0031-9007"))) {
    println(article.bibtex.toBIBTEXString)
  }

  // done
  //  for (article <- Search.inTopJournalsJumbled(50)) {
  //    println(article.bibtex.toBIBTEXString)
  //  }

  for (article <- Search.inTopJournalsJumbled(100)) {
    println(article.bibtex.toBIBTEXString)
  }

  FirefoxSlurp.quit
}

