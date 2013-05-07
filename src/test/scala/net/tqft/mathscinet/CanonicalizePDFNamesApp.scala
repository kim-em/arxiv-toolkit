package net.tqft.mathscinet

import java.io.File
import java.io.FilenameFilter
import net.tqft.util.FirefoxSlurp
import net.tqft.journals.ISSNs

object CanonicalizePDFNamesApp extends App {
  //  FirefoxSlurp.disable
  Article.disableBibtexSaving

  val directory = new File(System.getProperty("user.home") + "/scratch/elsevier-oa/")

  val identifierRegex = "MR[0-9]*".r

  val articles = (for (
    a <- Articles.fromBibtexGzipFile(System.getProperty("user.home") + "/projects/arxiv-toolkit/100_4.bib.gz");
    issn <- a.ISSNOption;
    if ISSNs.Elsevier.contains(issn)
  ) yield { a.identifierString -> a }).toMap

//   Verifying that we can build a name for *everything*
//    for (a <- Articles.fromBibtexGzipFile(System.getProperty("user.home") + "/projects/arxiv-toolkit/100_4.bib.gz")) {
//      try {
//        println(a.constructFilename())
//      } catch {
//        case e: Exception => {
//          e.printStackTrace()
//          println(a.bibtex.toBIBTEXString)
//          ???
//        }
//      }
//    }

  for (
    file <- directory.listFiles(new FilenameFilter { override def accept(dir: File, name: String) = identifierRegex.findFirstMatchIn(name).nonEmpty });
    identifier <- identifierRegex.findFirstIn(file.getName());
    article = articles.get(identifier).getOrElse(Article(identifier))
  ) {
    val newName = article.constructFilename()
    if (file.getName != newName) {
      println("Renaming " + file.getName + " to " + newName)
      file.renameTo(new File(directory, newName))
    }
  }
}