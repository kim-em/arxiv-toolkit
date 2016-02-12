package net.tqft.mathscinet

import net.tqft.util.Throttle
import net.tqft.eigenfactor.Eigenfactor
import net.tqft.util.FirefoxSlurp
import net.tqft.journals.ISSNs
import scala.util.Random

object SearchApp extends App {

  println(Search.inTopJournalsJumbled(200).size)
  println(Search.during(2013).size)
  
  //  println(Search.inJournal(ISSNs.`Journal of K-Theory`).size)
  //  for(a<- Search.byAuthorIdentifier(105700)) println(a)
  println(Search.institution("5-ANU").size)
  println(Search.institution("5-ANU-MI").size)
  println(Search.institution("5-ANU-CMA").size)
  FirefoxSlurp.quit
}

