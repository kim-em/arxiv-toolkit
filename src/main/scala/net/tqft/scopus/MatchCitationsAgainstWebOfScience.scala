package net.tqft.scopus

import net.tqft.webofscience
import net.tqft.scholar.Scholar
import scala.collection.mutable.ListBuffer

object MatchCitationsAgainstWebOfScience {
  def apply(scopusArticle: Article): (Seq[(Article, webofscience.Citation)], Seq[Article], Seq[webofscience.Citation]) = {
    scopusArticle.onWebOfScience match {
      case Some(onWebOfScience) => apply(scopusArticle, onWebOfScience)
      case None => (Seq.empty, scopusArticle.citations, Seq.empty)
    }
  }

  def apply(scopusArticle: Article, webOfScienceArticle: webofscience.Article): (Seq[(Article, webofscience.Citation)], Seq[Article], Seq[webofscience.Citation]) = {
    apply(scopusArticle.citations, webOfScienceArticle.citations)
  }

  // takes a sequence of Scopus articles, and sequence of WoS citations
  // returns a (X, Y, Z), where X is a list of matching pairs, Y is a list of unmatched Scopus articles, and Z is a list of unmatched WoS citations.
  def apply(scopusArticles: Seq[Article], webOfScienceCitations: Seq[webofscience.Citation]): (Seq[(Article, webofscience.Citation)], Seq[Article], Seq[webofscience.Citation]) = {
    val identifiers = Seq[(Article => Option[String], webofscience.Citation => Option[String])](
       ({ a: Article => a.DOIOption }, { c: webofscience.Citation => c.DOIOption }),
       ({ a: Article => a.PubMedIdOption }, { c: webofscience.Citation => c.PubMedIdOption }),
       ({ a: Article => Some(a.title.trim.replaceAll("[^a-zA-Z0-9]", "").toLowerCase) }, { c: webofscience.Citation => Some(c.title.trim.replaceAll("[^a-zA-Z0-9]", "").toLowerCase) }),
       ({ a: Article => Scholar(a).map(_.cluster) },{ c: webofscience.Citation => Scholar(c).map(_.cluster) })       
    )
    
    val remainingScopusArticles = ListBuffer() ++ scopusArticles
    val remainingWebOfScienceCitations = ListBuffer() ++ webOfScienceCitations
    val matchedPairs = ListBuffer[(Article, webofscience.Citation)]()

    for(identifier <- identifiers) {
      val scopusIdentifiers = (remainingScopusArticles.groupBy(identifier._1) - None)
      val webOfScienceIdentifiers = remainingWebOfScienceCitations.groupBy(identifier._2) - None
      val matchingIdentifiers = scopusIdentifiers.keySet.intersect(webOfScienceIdentifiers.keySet)
      for(id <- matchingIdentifiers) {
        matchedPairs += ((scopusIdentifiers(id).head, webOfScienceIdentifiers(id).head))
        remainingScopusArticles --= scopusIdentifiers.values.flatten
        remainingWebOfScienceCitations --= webOfScienceIdentifiers.values.flatten
      }
    }
    
    (matchedPairs.toSeq, remainingScopusArticles.toSeq, remainingWebOfScienceCitations.toSeq)
    
  }
}