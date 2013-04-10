package net.tqft.mathscinet

import net.tqft.toolkit.amazon.S3
import scala.collection.parallel.ForkJoinTaskSupport
import net.tqft.util.FirefoxSlurp

object CleanBIBTEX extends App {
  FirefoxSlurp.disable
  
  Article.disableBibtexSaving
  
  val pool = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(100))

  for (group <- Articles.fromBibtexFile("/Users/scott/projects/arxiv-toolkit/50.bib").grouped(1000); article <- { val p = group.par; p.tasksupport = pool; p }; bibtex = article.bibtex.toBIBTEXString; if bibtex.contains("&amp;") || bibtex.contains("&gt;") || bibtex.contains("&lt;")) {
    println(bibtex)
  }
}