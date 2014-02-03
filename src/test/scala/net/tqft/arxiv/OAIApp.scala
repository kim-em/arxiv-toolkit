package net.tqft.arxiv

object OAIApp extends App {
	for(r <- OAI.records(); if (scala.xml.XML.loadString(r) \\ "journal-ref").text.nonEmpty) println(r)
}