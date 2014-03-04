package net.tqft.util

object Similarity {

  // some documentation here: http://www.coli.uni-saarland.de/courses/LT1/2011/slides/stringmetrics.pdf

  def jaroWinkler(s: String, t: String) = {
    import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler
    new JaroWinkler().getSimilarity(s, t)
  }
  def levenshtein(s: String, t: String) = {
    import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein
    new Levenshtein().getSimilarity(s, t)
  }
}