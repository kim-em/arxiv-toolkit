package net.tqft.mathscinet

import net.tqft.toolkit.amazon.S3
import scala.collection.parallel.ForkJoinTaskSupport

object PopulateDOI2mathscinet extends App {
  val bucket = S3("DOI2mathscinet")

  Article.disableBibtexSaving

  //  println("found " + Articles.fromBibtexFile("/Users/scott/projects/arxiv-toolkit/50.bib").size + " bibtex entries")
  //  println("found " + Articles.fromBibtexFile("/Users/scott/projects/arxiv-toolkit/50.bib").count(_.DOI.nonEmpty) + " bibtex entries with a DOI")

  val pool = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(50))

  for (group <- Articles.fromBibtexFile("/Users/scott/projects/arxiv-toolkit/50.bib").grouped(1000); article <- { val p = group.par; p.tasksupport = pool; p }; doi <- article.DOI) {
    println(doi -> article.identifierString)
    bucket.put(doi, article.identifierString)
  }
}