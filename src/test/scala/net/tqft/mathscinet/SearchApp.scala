package net.tqft.mathscinet

import net.tqft.util.Throttle
import net.tqft.eigenfactor.Eigenfactor
import net.tqft.util.FirefoxSlurp
import net.tqft.journals.ISSNs

object SearchApp extends App {

  for (article <- Search.inTopJournalsJumbled(200)) {
    println(article.bibtex.toBIBTEXString)
  }

  // K-Theory
  for (article <- Search.inJournalsJumbled(Seq(ISSNs.`K-Theory`))) {
    println(article.bibtex.toBIBTEXString)
  }

  val `Advances, open access` = Search.query("arg3=&co4=AND&co5=NOT&co6=AND&co7=AND&dr=pubyear&extend=1&pg4=TI&pg5=JOUR&pg6=JOUR&pg7=ALLF&pg8=ET&review_format=html&s4=&s5=0001-8708&s6=Advancement&s7=&s8=All&vfpref=html&yearRangeFirst=&yearRangeSecond=2008&yrop=eq").toStream
  for (a <- `Advances, open access`) {
    println(a.bibtex.toBIBTEXString)
  }

  // Topology
  for (article <- Search.inJournalsJumbled(Seq(ISSNs.`Topology`))) {
    println(article.bibtex.toBIBTEXString)
  }

  //   Annals
  for (article <- Search.inJournal("0003-486X")) {
    println(article.bibtex.toBIBTEXString)
  }

  for (article <- Search.inTopJournalsJumbled(50)) {
    println(article.bibtex.toBIBTEXString)
  }

  // Crelle's journal
  for (article <- Search.inJournal("0075-4102")) {
    println(article.bibtex.toBIBTEXString)
  }

  // IMRN
  for (article <- Search.inJournal("1073-7928")) {
    println(article.bibtex.toBIBTEXString)
  }

  // AMS
  for (article <- Search.inJournalsJumbled(Seq("0002-9947", "1056-3911", "1073-7928", "0894-0347", "0075-4102", "1088-4173", "0273-0979", "0002-9939", "0025-5718", "0024-6107", "0002-9904", "0234-0852", "1088-4165", "0065-9266"))) {
    println(article.bibtex.toBIBTEXString)
  }

  // Elsevier
  for (article <- Search.inJournalsJumbled(ISSNs.Elsevier)) {
    println(article.bibtex.toBIBTEXString)
  }

  // LMS
  for (article <- Search.inJournalsJumbled(Seq("1753-8416", "0024-6093", "0024-6107", "0024-6115"))) {
    println(article.bibtex.toBIBTEXString)
  }

  // PNAS
  for (article <- Search.inJournal("1091-6490")) {
    println(article.bibtex.toBIBTEXString)
  }

  // Physical Review
  for (article <- Search.inJournalsJumbled(Seq("0034-6861", "1050-2947", "0163-1829", "0556-2813", "1550-7998", "0556-2821", "1539-3755", "0031-9007", "0031-9007"))) {
    println(article.bibtex.toBIBTEXString)
  }

  for (article <- Search.inTopJournalsJumbled(100)) {
    println(article.bibtex.toBIBTEXString)
  }

  FirefoxSlurp.quit
}

