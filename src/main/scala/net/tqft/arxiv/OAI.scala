package net.tqft.arxiv

import net.tqft.util.HttpClientSlurp
import net.tqft.util.URLEncode

object OAI {
  // OAI help at http://arxiv.org/help/oa/index
  // sample record at http://export.arxiv.org/oai2?verb=GetRecord&identifier=oai:arXiv.org:1009.5025&metadataPrefix=arXivRaw
  
	def records(from: Option[String] = None, set: String = "math", format: String = "arXivRaw") = {
	  for(response <- rawResponses(from, set, format); record <- (scala.xml.XML.loadString(response) \\ "record").iterator) yield {
	    record.toString
	  }
	}
  
	def rawResponses(from: Option[String] = None, set: String = "math", format: String = "arXivRaw"): Iterator[String] = {
	  def url(resumptionToken: Option[String]) = {
	    resumptionToken match {
	      case None =>"http://export.arxiv.org/oai2?verb=ListRecords&set=" + set + "&metadataPrefix=" + format + from.map("&from=" + _).getOrElse("")
	      case Some(t) =>"http://export.arxiv.org/oai2?verb=ListRecords&resumptionToken=" + URLEncode(t)
	    }
	  }
	  
	  def get(resumptionToken: Option[String]) = {
	    var response = HttpClientSlurp.getString(url(resumptionToken))
	    while(response.size < 1000 && response.contains("<h1>Retry after ")) {
	      val delay = response.split("\n")(1).stripPrefix("<h1>Retry after ").split(" ")(0).toInt
	      println("Sleeping for " + delay + " seconds, per OAI endpoint request.")
	      Thread.sleep(delay * 1000)
	      response = HttpClientSlurp.getString(url(resumptionToken))
	    }
	    response
	  }
	  
	  val initial = HttpClientSlurp.getString(url(None))
	  
	  def extractResumptionToken(response: String): Option[String] = {
	    val token = (scala.xml.XML.loadString(response) \\ "resumptionToken")
	    println("Received resumption token: " + token)
	    token.text match {
	      case "" => None
	      case other => Some(other)
	    }
	  }
	  
	  def next(response: String): Option[String] = {
	    if(response.size < 1000) println(response)
	    extractResumptionToken(response).map(t => get(Some(t)))
	  }
	  
	  import net.tqft.toolkit.collections.Iterators._
	  Iterator.iterateUntilNone(initial)(next)
	}
}