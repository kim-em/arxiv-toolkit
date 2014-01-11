package net.tqft.arxiv

object Identifiers {
	def extractVersionNumber(identifier: String): Option[Int] = {
	  val re = ".*v([1-9][0-9]*)".r
	  identifier match {
	    case re(i) => Some(i.toInt)
	    case _ => None
	  }
	}
	def stripVersionNumber(identifier: String): String = {
	  val re = "(.*)v[1-9][0-9]*".r
	  identifier match {
	    case re(stripped) => stripped
	    case _ => identifier
	  }
	}
	def stripSubjectArea(identifier: String): String = {
	  ???
	}
}