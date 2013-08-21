package net.tqft.mathscinet

import java.io.File
import net.tqft.journals.ISSNs

object SymlinkApp extends App {

  Article.disableBibtexSaving

  val source = new File("/Volumes/Repository Backups/elsevier-oa/")
  val target = new File(System.getProperty("user.home") + "/Literature")

  val journals = Map("Topology" -> ISSNs.`Topology`)

  for ((name, issn) <- journals) {

    for (article <- Search.inJournal(issn); y <- article.yearOption; if y <= 2008) {
      val sourceFile = new File(source, article.constructFilename())
      if (!sourceFile.exists) {
        println("Not found in source directory: ")
        println(article.bibtex.toBIBTEXString)
        article.savePDF(source)
      } else {
        val targetFile = new File(new File(target, name), article.constructFilename())
        if (!targetFile.exists) {
          println("Not found in target directory: ")
          println(article.constructFilename())
          println(article.bibtex.toBIBTEXString)
        }
      }
      // Not finished!
    }

  }
}