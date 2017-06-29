package net.tqft.mathscinet

import net.tqft.toolkit.Logging
import net.tqft.toolkit.Extractors.Int

object WalkCitations extends App {
  val counts = scala.collection.mutable.Map.empty[Int, Double]
  val citeCount = scala.collection.mutable.Map.empty[Int, Double]
  val done = scala.collection.mutable.Set.empty[Int] // ++ Articles.identifiersInDatabase

  for (Int(i) <- args) counts(i) = 0.0
  while (args.isEmpty || counts.nonEmpty) {
    if (args.isEmpty) counts(scala.util.Random.nextInt(3000000)) = 0.0

    while (counts.nonEmpty) {
      val next = {
        val max = counts.values.max
        val top = counts.filter(_._2 == max).toSeq
        val i = scala.util.Random.nextInt(top.size)
        top(i)._1
      }
      try {
        val article = Article(next)
        val citations = article.citations.toSeq
        val weight = (citations.size + 1.0) / (2020 - article.yearOption.getOrElse(1990))
        for (cite <- citations; id = cite.identifier; if !done.contains(id)) {
          counts(id) = counts.get(id).getOrElse(0.0) + weight
        }
        val refs = article.bestReferenceMathSciNetMatches.map(_._2) 
        for (ref <- refs; id = ref.identifier; if !done.contains(id)) {
          counts(id) = counts.get(id).getOrElse(0.0) + weight * (citations.size + 1) / (refs.size + 1)
        }

      } catch {
        case e if e.getMessage == null || e.getMessage.startsWith("500") => {
          Logging.warn(e)
          Logging.warn(e.getMessage)
          e.printStackTrace()
          Logging.warn("Sleeping for 20 minutes")
          Thread.sleep(1200000L)
        }
        case e: Throwable => Logging.warn(e)
      }

      done += next
      counts -= next
    }
  }
}