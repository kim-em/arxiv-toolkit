package net.tqft.mathscinet

import net.tqft.journals.Journal
import net.tqft.journals.ISSNs
import net.tqft.portico.Portico
import net.tqft.util.FirefoxSlurp

object KTheory extends App {
	val articles = Journal(ISSNs.`K-Theory`).articles
	println(articles.size)
	println(articles.count(_.DOI.nonEmpty))
	
	for(result <- Portico.scrape("http://www.portico.org/Portico/#!journalAUView/cs=ISSN_09203036?ct=E-Journal%20Content?label=v.%2035,%20n.%201-2%20(June,%202005)")) {
	  println(result)
	}
	
	FirefoxSlurp.quit
}