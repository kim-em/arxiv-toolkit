package net.tqft.eigenfactor

import net.tqft.util.Slurp

object Eigenfactor {
	def topJournals = {
	  val ISSN = "[0-9]{4}-[0-9X]{4}".r
	  Slurp("http://www.eigenfactor.org/rankings.php?bsearch=PQ&searchby=category&orderby=eigenfactor").flatMap(ISSN.findAllIn).toSeq.distinct
	}
}