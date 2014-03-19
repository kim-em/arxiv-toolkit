package net.tqft.scopus

import net.tqft.util.BIBTEX
import net.tqft.util.Slurp
import net.tqft.toolkit.Logging

case class Article(id: String, title: String) {
  def URL = "http://www.scopus.com/record/display.url?eid=" + id + "&origin=resultslist"
  def textURL = "http://www.scopus.com/onclick/export.url?oneClickExport=%7b%22Format%22%3a%22TEXT%22%2c%22View%22%3a%22FullDocument%22%7d&origin=recordpage&eid=" + id + "&zone=recordPageHeader&outputType=export"
  def bibtexURL = "http://www.scopus.com/onclick/export.url?oneClickExport=%7b%22Format%22%3a%22BIB%22%2c%22View%22%3a%22FullDocument%22%7d&origin=recordpage&eid=" + id + "&zone=recordPageHeader&outputType=export"
  
  lazy val dataText = Slurp(textURL).toStream
  
  def citationOption = "(.*) Cited [0-9]* times.".r.findFirstMatchIn(dataText(5).trim).map(_.group(1))
  
//  private var bibtexData: Option[BIBTEX] = None
//  def bibtex = {
//    if (bibtexData.isEmpty) {
  // // Fails because Scopus forces us to download...
//      val text = Slurp(bibtexURL).toList.drop(3).mkString("\n")
//      Logging.info("Found BIBTEX for " + id + ":\n" + text)
//      try {
//        bibtexData = BIBTEX.parse(text)
//      } catch {
//        case e: Exception => {
//          Logging.error("Exception while parsing BIBTEX for " + id + ": \n" + text, e)
//          try {
//            Slurp -= bibtexURL
//          } catch {
//            case e: Exception => Logging.warn("Failed to clean slurp database.", e)
//          }
//          ???
//        }
//      }
//    }
//    bibtexData.get
//  }

}