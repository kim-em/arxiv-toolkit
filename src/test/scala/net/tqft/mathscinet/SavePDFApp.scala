package net.tqft.mathscinet

import net.tqft.util.FirefoxSlurp
import java.io.File
import java.net.URL
import net.tqft.journals.ISSNs

object SavePDFApp extends App {
  FirefoxSlurp.disable
  Article.disableBibtexSaving

  def articles = Articles.fromBibtexFile(System.getProperty("user.home") + "/projects/arxiv-toolkit/100_4.bib")
  //    def articles = Search.inJournalsJumbled(ISSNs.Elsevier)

  def openAccessElsevierArticles = for (a <- articles; issn <- a.ISSNOption; if ISSNs.Elsevier.contains(issn); y <- a.yearOption; if y <= 2008; doi <- a.DOI) yield a
  def openAccessAdvancesArticles = for (a <- articles; issn <- a.ISSNOption; if issn == ISSNs.`Advances in Mathematics`; if a.journal != "Advancement in Math."; y <- a.yearOption; if y <= 2008) yield a
  def openAccessTopologyArticles = for (a <- articles; issn <- a.ISSNOption; if issn == ISSNs.`Topology`; y <- a.yearOption; if y <= 2008) yield a
  
//  def KTheory = for (a <- articles; issn <- a.ISSNOption; if issn == "0920-3036") yield a
  def KTheory = Search.inJournalsJumbled(Seq(ISSNs.`K-Theory`))

  val missingMRNumbers = Seq()
  val missing = articles.filter(a => missingMRNumbers.contains(a.identifierString))

  //  println(openAccessAdvancesArticles.map(_.year).min)
  //  println(openAccessAdvancesArticles.filter(_.pdfURL.nonEmpty).map(_.year).min)
  //  println(openAccessAdvancesArticles.filter(_.pdfURL.isEmpty).map(_.year).max)

//  val dir = new File(System.getProperty("user.home") + "/scratch/elsevier-oa/")
//  val dir = new File("/Volumes/Repository Backups/elsevier-oa/")
  val dir = new File(System.getProperty("user.home") + "/k-theory/")
  
  val alreadyDownloaded = dir.listFiles().iterator.map(_.getName().stripSuffix(".pdf").takeRight(9)).toSet
  
  for (article <- /* openAccessElsevierArticles*/ KTheory; if !alreadyDownloaded.contains(article.identifierString)) {
    Thread.sleep(150000)
    try {
      println(article.bibtex.toBIBTEXString)
      article.savePDF(dir)
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

}