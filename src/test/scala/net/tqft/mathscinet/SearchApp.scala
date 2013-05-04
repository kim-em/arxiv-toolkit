package net.tqft.mathscinet

import net.tqft.util.Throttle
import net.tqft.eigenfactor.Eigenfactor
import net.tqft.util.FirefoxSlurp

object SearchApp extends App {

//  // Crelle's journal
//  for (article <- Search.inJournal("0075-4102")) {
//    println(article.bibtex.toBIBTEXString)
//  }
//
//  // IMRN
//  for (article <- Search.inJournal("1073-7928")) {
//    println(article.bibtex.toBIBTEXString)
//  }
//
//  // AMS
//  for (article <- Search.inJournalsJumbled(Seq("0002-9947", "1056-3911", "1073-7928", "0894-0347", "0075-4102", "1088-4173", "0273-0979", "0002-9939", "0025-5718", "0024-6107", "0002-9904", "0234-0852", "1088-4165", "0065-9266"))) {
//    println(article.bibtex.toBIBTEXString)
//  }
 
	  // Elsevier
	  for (article <- Search.inJournalsJumbled(Seq("0196-8858", "0001-8708", "0294-1449", "0003-4843", "0168-0072", "1063-5203", "0307-904X", "0893-9659", "0007-4497", "0925-7721", "0898-1221", "0926-2245", "0166-218X", "0012-365X", "1572-5286", "0195-6698", "0723-0869", "1071-5797", "0016-660X", "0315-0860", "0019-3577", "1385-7258", "0019-9958", "0890-5401", "0888-613X", "0021-7824", "0021-8693", "1570-8683", "0021-9045", "0021-9800", "0097-3165", "0095-8956", "0885-064X", "0377-0427", "0022-0000", "0022-0396", "1570-8667", "0022-1236", "0743-1066", "1567-8326", "0022-247X", "0047-259X", "0022-314X", "0022-4049", "0747-7171", "0024-3795", "0895-7177", "0270-0255", "0167-6423", "0304-4149", "0304-3975", "0040-9383", "0166-8641"))) {
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

    // ---------------done-------------------
   // Annals
  //  for(article <- Search.inJournal("0003-486X")) {
  //     println(article.bibtex.toBIBTEXString)   
  //  }

 //  for (article <- Search.inTopJournalsJumbled(50)) {
  //    println(article.bibtex.toBIBTEXString)
  //  }


  
  FirefoxSlurp.quit
}

