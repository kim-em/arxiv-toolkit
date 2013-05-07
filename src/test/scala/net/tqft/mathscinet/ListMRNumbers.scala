package net.tqft.mathscinet

import net.tqft.toolkit.amazon.S3
import scala.collection.parallel.ForkJoinTaskSupport
import net.tqft.util.FirefoxSlurp
import net.tqft.journals.ISSNs
import java.io.File
import java.io.FilenameFilter
import java.io.PrintWriter
import java.io.FileOutputStream

object ListMRNumbers extends App {
  FirefoxSlurp.disable
  Article.disableBibtexSaving

  val pool = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(100))

  val directory = new File(System.getProperty("user.home") + "/scratch/elsevier-oa/")

  val pw = new PrintWriter(new FileOutputStream(new File(directory, "advances.bib")))
  
  val `Advances, open access` = Search.query("arg3=&co4=AND&co5=NOT&co6=AND&co7=AND&dr=pubyear&extend=1&pg4=TI&pg5=JOUR&pg6=JOUR&pg7=ALLF&pg8=ET&review_format=html&s4=&s5=0001-8708&s6=Advancement&s7=&s8=All&vfpref=html&yearRangeFirst=&yearRangeSecond=2008&yrop=eq").toStream
  for(a <- `Advances, open access`) {
    pw.println(a.bibtex.toBIBTEXString)
  }
  
  
//  for (
//    group <- Articles.fromBibtexGzipFile(System.getProperty("user.home") + "/projects/arxiv-toolkit/100_4.bib.gz").grouped(1000);
//    article <- { val p = group.par; p.tasksupport = pool; p };
//    bibtex = article.bibtex.toBIBTEXString;
//    issn <- article.ISSNOption;
//    if issn == ISSNs.`Advances in Mathematics`;
//    if article.journal != "Advancement in Math.";
////    if issn == ISSNs.`Topology`;
//    year <- article.yearOption;
////    if directory.listFiles(new FilenameFilter { override def accept(dir: File, name: String) = name.contains(article.identifierString) }).isEmpty;
//    if year <= 2008
//  ) {
//    try {
//      println(article.identifierString)
////      println(bibtex)
////      article.savePDF(directory)
//    } catch {
//      case e: Exception => e.printStackTrace()
//    }
//  }
}