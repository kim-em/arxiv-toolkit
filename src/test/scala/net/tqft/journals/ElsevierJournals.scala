package net.tqft.journals

object ElsevierJournals extends App {
  for(issn <- ISSNs.Elsevier) {
    try { println(Journals.names(issn) + " " + issn.replaceAll("-", "")) } catch { case e: Exception => }
  }
}