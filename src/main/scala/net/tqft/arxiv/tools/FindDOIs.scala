package net.tqft.arxiv.tools

import net.tqft.arxiv._
import net.tqft.mathscinet.MRef

object FindDOIs {
  // returns a mapping, arxiv identifiers to DOIs
  def forAuthor(name: String): Map[String, String] = {
    (for (
      author <- Author.lookup(name).toList;
      article <- author.articles;
      doi <- article.currentVersion.DOI.orElse(MRef.lookupArXivArticle(article).flatMap(_.DOI))
    ) yield {
      article.identifier -> doi
    }).toMap
  }
}