package net.tqft.mathscinet

import net.tqft.util.Throttle
import net.tqft.eigenfactor.Eigenfactor
import net.tqft.util.FirefoxSlurp
import net.tqft.journals.ISSNs

object SearchApp extends App {
//  println(Search.inTopJournalsJumbled(200).size)
//  println(Search.during(2013).size)
  println(Search.inJournalsJumbled(Articles.ISSNsInDatabase).size)
  

  FirefoxSlurp.quit
}

