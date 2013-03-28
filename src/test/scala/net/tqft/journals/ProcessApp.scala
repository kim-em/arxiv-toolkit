package net.tqft.journals

import net.tqft.mathscinet.Articles
import net.tqft.util.BIBTEX
import net.tqft.mathscinet.Article
import net.tqft.mathscinet.Search

object ProcessApp extends App {
	val article = Search.by("Izumi").filter(_.DOI.nonEmpty).drop(5).next
	
	Process(article.DOI.get)
}