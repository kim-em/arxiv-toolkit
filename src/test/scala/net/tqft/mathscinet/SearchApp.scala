package net.tqft.mathscinet

import net.tqft.util.Throttle
import net.tqft.eigenfactor.Eigenfactor
import net.tqft.util.FirefoxSlurp
import net.tqft.journals.ISSNs

object SearchApp extends App {
//  println(Search.inTopJournalsJumbled(200).size)
//  println(Search.during(2013).size)
//  println(Search.inJournalsJumbled(Articles.ISSNsInDatabase).size)
  
//  println(Search.inJournal(ISSNs.`Journal of K-Theory`).size)
  println(Search.byAuthorIdentifier(105700))

  FirefoxSlurp.quit
}

