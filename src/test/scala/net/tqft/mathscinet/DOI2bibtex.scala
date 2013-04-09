package net.tqft.mathscinet

object DOI2bibtex extends App {
  val doi = "10.1090/S0002-9904-1897-00411-6"
	println(Article.fromDOI(doi).map(_.bibtex.toBIBTEXString))
}