package net.tqft.mathscinet

import java.io.File
import java.io.FilenameFilter
import net.tqft.util.FirefoxSlurp
import net.tqft.journals.ISSNs

object CanonicalizePDFNamesApp extends App {
  //  FirefoxSlurp.disable
  Article.disableBibtexSaving

  val directory = new File("/Volumes/Repository Backups/elsevier-oa/")

  val identifierRegex = "MR[0-9]{7}".r

  val articles = (for (
    a <- Articles.fromBibtexFile(System.getProperty("user.home") + "/projects/arxiv-toolkit/100_4.bib");
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

  //   Looking at everything with two identifiers in the name!
//  for (
//    (_, a) <- articles;
//    filename = a.constructFilename();
//    identifiers = identifierRegex.findAllMatchIn(filename).toSeq;
//    if identifiers.size > 1
//  ) {
//    println(a.constructFilename())
//    for (id <- identifiers.map(_.matched)) {
//      println(id)
//    }
//  }

    for (
      file <- directory.listFiles(new FilenameFilter { override def accept(dir: File, name: String) = identifierRegex.findFirstMatchIn(name).nonEmpty });
      identifier <- identifierRegex.findAllMatchIn(file.getName()).toSeq.lastOption.map(_.matched);
      article = articles.get(identifier).getOrElse(Article(identifier))
    ) {
      val newName = article.constructFilename()
      if (file.getName != newName) {
        println("Renaming " + file.getName + " to " + newName)
        file.renameTo(new File(directory, newName))
      }
    }
}