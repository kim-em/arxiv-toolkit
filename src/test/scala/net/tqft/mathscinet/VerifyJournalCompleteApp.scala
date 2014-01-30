package net.tqft.mathscinet

import java.io.File
import net.tqft.journals.ISSNs
import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import net.tqft.util.pandoc
import net.tqft.journals.Journals
import java.nio.file.Files
import java.nio.file.DirectoryStream
import java.nio.file.Path
import java.nio.file.Paths
import net.tqft.journals.Journal
import net.tqft.toolkit.Logging

object VerifyJournalCompleteApp extends App {

  Article.disableBibtexSaving

  val source = Paths.get("/Volumes/Repository Backups/elsevier-oa/")
  val target = Paths.get(System.getProperty("user.home") + "/Literature")

  val elsevierJournals = Journals.names.filter(p => ISSNs.Elsevier.contains(p._1))

  def pdfs(directory: Path) = {
    import scala.collection.JavaConverters._

    val identifierRegex = "MR[0-9]{7}".r

    Files.newDirectoryStream(directory, new DirectoryStream.Filter[Path] {
      override def accept(path: Path) = {
        identifierRegex.findFirstMatchIn(path.getFileName.toString).nonEmpty
      }
    }).iterator.asScala
  }

  for ((issn, name) <- elsevierJournals.headOption) {

    println("Checking " + name)

    val journalPath = target.resolve(name)

    if (!Files.exists(journalPath)) {
      Files.createDirectory(journalPath)
      if (Files.exists(source.resolve("LICENSE.pdf"))) {
        Files.createSymbolicLink(journalPath.resolve("LICENSE.pdf"), source.resolve("LICENSE.pdf"))
      }
    }

    val files = scala.collection.mutable.Set[Path]() ++ pdfs(journalPath)

    var count = 0
    for (article <- Journal(issn).upToYear(2008); if article.journalOption != Some("Advancement in Math.")) {
      count = count + 1
      val filename = article.constructFilename()
      val sourceFile = source.resolve(filename)
      if (!Files.exists(sourceFile)) {
        println("Not found in source directory: ")
        println(filename)
        println(article.bibtex.toBIBTEXString)
        article.savePDF(source.toFile())
      }
      if (Files.exists(sourceFile)) {
        val targetFile = journalPath.resolve(filename)
        if (!Files.exists(targetFile)) {
          println("Not found in target directory: ")
          println(filename)
          println(article.bibtex.toBIBTEXString)
          try {
            println("Creating symbolic link ...")
            Files.createSymbolicLink(targetFile, sourceFile)
          } catch {
            case e: Exception => {
              Logging.warn("Exception while creating symbolic link for:\n" + article.bibtex.toBIBTEXString, e)
            }
          }
        } else {
          files -= targetFile
        }
      }
    }

    println("Found " + count + " articles in " + name)
    if (files.nonEmpty) {
      println("There were some files that perhaps shouldn't be there, deleting now...")
      for (file <- files) {
        println(file.getFileName)
        Files.delete(file)
      }
    }
  }
}