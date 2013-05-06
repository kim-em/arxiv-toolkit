package net.tqft.mathscinet

import net.tqft.toolkit.amazon.S3
import scala.collection.parallel.ForkJoinTaskSupport
import net.tqft.util.FirefoxSlurp
import net.tqft.journals.ISSNs
import java.io.File
import java.io.FilenameFilter

object ListMRNumbers extends App {
  FirefoxSlurp.disable
  Article.disableBibtexSaving

  val pool = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(100))

  val directory = new File(System.getProperty("user.home") + "/scratch/elsevier-oa/")

  for (
    group <- Articles.fromBibtexGzipFile(System.getProperty("user.home") + "/projects/arxiv-toolkit/100_4.bib.gz").grouped(1000);
    article <- { val p = group.par; p.tasksupport = pool; p };
    bibtex = article.bibtex.toBIBTEXString;
    issn <- article.ISSNOption;
    if issn == ISSNs.`Advances in Mathematics`;
    if article.journal != "Advancement in Math.";
//    if issn == ISSNs.`Topology`;
    year <- article.yearOption;
//    if directory.listFiles(new FilenameFilter { override def accept(dir: File, name: String) = name.contains(article.identifierString) }).isEmpty;
    if year <= 2008
  ) {
    try {
      println(article.identifierString)
//      println(bibtex)
//      article.savePDF(directory)
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }
}