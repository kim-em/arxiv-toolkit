package net.tqft.mathscinet

import net.tqft.util.FirefoxSlurp
import java.io.File
import java.net.URL
import net.tqft.journals.ISSNs

object SavePDFApp extends App {
  FirefoxSlurp.disable
  Article.disableBibtexSaving

  //  def articles = Articles.fromBibtexFile(System.getProperty("user.home") + "/projects/arxiv-toolkit/100_4.bib")
  def articles = Search.inJournalsJumbled(ISSNs.Elsevier)

  def openAccessElsevierArticles =
    for (
      a <- articles;
      if a.bibtex.documentType == "article";
      issn <- a.ISSNOption;
      if ISSNs.Elsevier.contains(issn);
      y <- a.yearOption;
      if y <= 2008;
      if issn != "0007-4497" || y >= 1998;
      if issn != "0021-7824" || y >= 1997;
      if issn != "0723-0869" || y >= 2001;
      if issn != "0019-3577" || y >= 1990;
      if issn != "0195-6698" || y >= 1992
    ) yield a
  def openAccessAdvancesArticles = for (a <- Search.inJournal(ISSNs.`Advances in Mathematics`); if a.journal != "Advancement in Math."; y <- a.yearOption; if y <= 2008) yield a
  def openAccessTopologyArticles = for (a <- Search.inJournal(ISSNs.`Topology`); y <- a.yearOption; if y <= 2008) yield a
  def openAccessJAlgebraArticles = for (a <- Search.inJournal(ISSNs.`Journal of Algebra`); y <- a.yearOption; if y <= 2008) yield a
  def openAccessDiscreteMathArticles = for (a <- Search.inJournal(ISSNs.`Discrete Mathematics`); y <- a.yearOption; if y <= 2008) yield a

  //  def KTheory = for (a <- articles; issn <- a.ISSNOption; if issn == "0920-3036") yield a
  def KTheory = Search.inJournalsJumbled(Seq(ISSNs.`K-Theory`))

  val missingMRNumbers = Seq()
  val missing = articles.filter(a => missingMRNumbers.contains(a.identifierString))

  //  println(openAccessAdvancesArticles.map(_.year).min)
  //  println(openAccessAdvancesArticles.filter(_.pdfURL.nonEmpty).map(_.year).min)
  //  println(openAccessAdvancesArticles.filter(_.pdfURL.isEmpty).map(_.year).max)

  //  val dir = new File(System.getProperty("user.home") + "/scratch/elsevier-oa/")
  val dir = new File("/Volumes/Repository Backups/elsevier-oa/")
  //  val dir = new File(System.getProperty("user.home") + "/k-theory/")

  lazy val alreadyDownloaded = dir.listFiles().iterator.map(_.getName().stripSuffix(".pdf").takeRight(9)).toSet

  val commandLineArticles = args.map(Article.apply)

  val targetArticles = if (commandLineArticles.nonEmpty) {
    commandLineArticles.iterator
  } else {
    openAccessElsevierArticles.filterNot(a => alreadyDownloaded.contains(a.identifierString))
  }

  for (article <- targetArticles; if !Seq("MR1863897", "MR1793590").contains(article.identifierString) /* awaiting explication */ ) {
    try {
      println(article.bibtex.toBIBTEXString)
      article.savePDF(dir)
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

}