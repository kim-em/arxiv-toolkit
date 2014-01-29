package net.tqft.mathscinet

import java.io.File
import net.tqft.journals.ISSNs
import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import net.tqft.util.pandoc
import net.tqft.journals.Journals

object VerifyJournalCompleteApp extends App {

  Article.disableBibtexSaving

  val source = new File("/Volumes/Repository Backups/elsevier-oa/")
  val target = new File(System.getProperty("user.home") + "/Literature")


  val elsevierJournals = Journals.names.filter(p => ISSNs.Elsevier.contains(p._1))

  def pdfs(directory: File) = {
    scala.sys.process.Process("ls", directory).lines_!.iterator.filter(name => name.endsWith(".pdf")).map(name => new File(directory, name))
  }

  for ((issn, name) <- elsevierJournals) {

    println("Checking " + name)

    val files = scala.collection.mutable.Set[File]() ++ pdfs(new File(target, name))

    var count = 0
    for (article <- Search.inJournal(issn); y <- article.yearOption; if y <= 2008; if article.journal != "Advancement in Math.") {
      count = count + 1
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
        } else {
          files --= files.find(_.getName.endsWith(article.identifierString + ".pdf"))
        }
      }
      // Not finished!
    }

    println("Found " + count + " articles in " + name)
    if (files.nonEmpty) {
      println("There were some files that perhaps shouldn't be there:")
      for (file <- files) { println(file.getName) }
    }
  }
}