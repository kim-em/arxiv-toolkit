package net.tqft.mathscinet
import scala.io.Source

object MRef {
	def lookup(reference: String): List[Article] = {
	  val re = """<tr><td align="left"><pre>&lt;a href="http://www.ams.org/mathscinet-getitem\?mr=([0-9]*)"&gt;[0-9]*&lt;/a&gt;</pre></td></tr>""".r
	  
	  Source.fromURL("http://ams.org/mathscinet-mref?dataType=link&ref=" + net.liftweb.util.Helpers.urlEncode(reference)).getLines().flatMap {
	    line => line match { case re(mr) => Some(Article(mr.toInt)); case _ => None }
	  }.toList
	  
	}
	def lookupArXivArticle(article: net.tqft.arxiv.Article): Option[Article] = {
	  lookup(article.currentVersion.title + "\n" + article.currentVersion.authors.map(_.name).mkString(", ") + "\n" + article.currentVersion.journalReference.getOrElse("")).headOption
	}
}