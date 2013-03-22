package net.tqft.mathscinet

object SearchApp extends App {
	for(article <- Search.query("pg4=AUCN&s4=morrison")) {
	  println(article.identifier -> article.DOI)
	}
}