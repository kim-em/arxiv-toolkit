package net.tqft.citation

import net.tqft.mathscinet.MRef

case class Citation(text: String) {
	def mrlookup: List[net.tqft.mathscinet.Article] = {
	  MRef.lookup(text)
	}
	def arxiv: Option[net.tqft.arxiv.Article] = ???
}