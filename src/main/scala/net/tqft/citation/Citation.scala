package net.tqft.citation

import net.tqft.mathscinet.MRef
import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import slick.jdbc.MySQLProfile.api._
import net.tqft.util.Similarity

case class Citation(text: String) {
	def mrlookup: List[net.tqft.mathscinet.Article] = {
	  MRef.lookup(text)
	}
	def arxiv: List[net.tqft.arxiv2.Article] = {
	  // * Look for an arxiv identifier in the text
	  // * Look it up on zbmath, and see if there's an arxiv link
	  // * Otherwise, try to find a DOI and check that against the arxiv metadata database
	  def found = {
	    if(text.toLowerCase.contains("arxiv")) {
	      val chunk = text.drop(text.toLowerCase.indexOf("arxiv") + 5).take(15)
	      val r1 = """math\.[A-Z]{2}/[0-9]{7}""".r
	      val r2 = """[-a-z]*/[0-9]{7}""".r
	      val r3 = """[0-9]{4}\.[0-9]{4}""".r
	      Some(r1.findFirstIn(chunk).orElse(r2.findFirstIn(chunk)).orElse(r2.findFirstIn(chunk)).get)
	    } else {
	      None
	    }
	  }
	  def fromZbl = zblMatch.flatMap(_.arxiv)
	  def byDOI: List[String] = doi.headOption match {
	    case Some(d) => {	      
	      (SQL {
	        (for(a <- SQLTables.arxiv; if a.doi === d) yield a.arxivid)
	      }).toList
	    }
	    case None => Nil
	  }
	  val results = found match {
	    case Some(r) => List(r)
	    case None => {
	      fromZbl match {
	        case Nil => {
	          byDOI
	        }
	        case r => r
	      }
	    }
	  }
	  val articles = results.flatMap(id => SQL { for(a <- SQLTables.arxiv; if a.arxivid === id) yield a } )
	  articles.sortBy(a => -Similarity.levenshtein(a.citation, text))
	}
	lazy val zblMatch = net.tqft.zentralblatt.CitationMatching(text)
	def zbmath: List[String] = zblMatch.map(_.zbl_id)
	def doi: List[String] = {
	  // First, just look for a DOI directly in the string.
	  def found: Option[String] = {
	    if(text.toLowerCase.contains("doi")) {
	      val tail = text.drop(text.toLowerCase.indexOf("doi") + 3)
	      val r = """10.[0-9]{4}/[-A-Za-z0-9/@()]*""".r //FIXME other characters?
	      Some(r.findFirstIn(tail).get)
	    } else {
	      None
	    }
	  }
	  def fromZbl = zblMatch.flatMap(_.doi)
	  def fromMR = mrlookup.flatMap(_.DOI)
	  found match {
	    case Some(r) => List(r)
	    case None => {
	      fromZbl match {
	        case Nil => fromMR
	        case r => r
	      }
	    }
	  }
	}
}