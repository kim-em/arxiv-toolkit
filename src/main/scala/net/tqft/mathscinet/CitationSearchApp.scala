package net.tqft.mathscinet

import scala.io.Source
import scala.collection.mutable.ListBuffer

object CitationSearchApp extends App {

  val N = 3000

  val index: Map[String, Set[Int]] = Source.fromFile("terms").getLines.grouped(2).map({ pair =>
    pair(0) -> pair(1).split(",").map(_.toInt).toSet
  }).toMap

  def idf(term: String): Option[Double] = {
    index.get(term) match {
      case None => None
      case Some(documents) => {
        val n = documents.size
        val r = scala.math.log((N - n + 0.5) / (n + 0.5))
        if (r < 0) {
          None
        } else {
          Some(r)
        }
      }
    }
  }

  def query(searchString: String) = {
    val terms = searchString.split(" ").map(_.stripPrefix("(").stripSuffix(")").stripSuffix(",").stripSuffix(".").toLowerCase).iterator.filter(_.nonEmpty).map(_.toLowerCase).toList
    val idfs: Seq[(String, Double)] = terms.map(t => t -> idf(t)).collect({ case (t, Some(q)) => (t, q) }).sortBy(p => -p._2)

    def score(documents: Set[Int]): Seq[(Int, Double)] = {
      val scores: Iterator[(Int, Double)] = (for (d <- documents.iterator) yield {
        d -> (for ((t, q) <- idfs) yield {
          if (index(t).contains(d)) q else 0.0
        }).sum
      })

      scores.toVector.sortBy({ p => -p._2 }).take(5)
    }

    (if (idfs.isEmpty) {
      Seq.empty
    } else {

      val sets = idfs.iterator.map({ case (t, q) => ((t, q), index(t)) }).toStream

      val intersections = sets.tail.scanLeft(sets.head._2.toSet)(_ intersect _._2)
      val j = intersections.indexWhere(_.isEmpty) match {
        case -1 => intersections.size
        case j => j
      }

      val qsum = idfs.drop(j).map(_._2).sum
      if (idfs(j - 1)._2 >= qsum) {
        // the other search terms don't matter
        score(intersections(j - 1))
      } else {

        // start scoring the unions, until we have a winner
        // TODO
        val unions = sets.scanLeft(Set[Int]())(_ union _._2)
        val diff = sets.map(_._2).zip(unions).map(p => p._1 diff p._2)
        var scored = scala.collection.mutable.Buffer[(Int, Double)]()

        val tailQSums = idfs.map(_._2).tails.map(_.sum).toStream
        var k = 0
        while (k < idfs.size && (scored.size < 2 || (scored(0)._2 - scored(1)._2 < tailQSums(k)))) {
          scored ++= score(diff(k))
          scored = scored.sortBy(p => -p._2)
          k += 1
        }

        scored.take(5).toSeq
        //        val documents = terms.iterator.map(index).foldLeft(Set[Int]())(_ ++ _)
        //        score(documents)
      }
    }) //.map({ case (i, q) => (Article(i), q) })
  }

  //  for(w <- Article(2914056).fullCitation.split(" ").map(_.stripPrefix("(").stripSuffix(")").stripSuffix(",").stripSuffix(".").toLowerCase)) {
  //    println(w)
  //  }

  val s = "Subfactors with index at most 5, part 1: the odometer, morrison, snyder, cmp"
  for (q <- s.reverse.tails.toSeq.reverse.map(_.reverse)) {
    println(q)
    println(query(q))
  }

}