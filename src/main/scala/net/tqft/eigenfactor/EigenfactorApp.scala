package net.tqft.eigenfactor

object EigenfactorApp extends App {
	for(issn <- Eigenfactor.topJournals) println(issn)
}