package net.tqft.mathscinet

import net.tqft.toolkit.amazon.S3
import scala.collection.parallel.ForkJoinTaskSupport
import net.tqft.util.FirefoxSlurp

object SearchLocalBIBTEX extends App {
  FirefoxSlurp.disable
  
  Article.disableBibtexSaving
  
  val pool = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(100))

 for (group <- Articles.fromBibtexFile(System.getProperty("user.home") + "/projects/arxiv-toolkit/100_4.bib").grouped(1000); article <- { val p = group.par; p.tasksupport = pool; p };
  	doi <- article.DOI;
  	if doi.startsWith("10.1090");
  	if article.bibtex.get("JOURNAL") == Some("Algebra i Analiz")
  	)  {
    println(article.bibtex.toBIBTEXString)
  }
}