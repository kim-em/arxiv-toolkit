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
import net.tqft.toolkit.Throttle

object VerifyJournalCompleteApp extends App {

  val missingButWhoCares_? = Seq("MR0337466", "MR0337465", "MR0337467", "MR1690573") ++ // birthdays
    Seq("MR0585438", "MR0585435", "MR0973821") ++ // bad data on mathscinet?
    Seq("MR0949339", "MR1895524") ++ // memorials
    Seq("MR0532067") // errata

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

  var journalCount = 0
  for ((issn, name) <- scala.util.Random.shuffle(elsevierJournals.toSeq)) {

    journalCount += 1
    println("Checking " + name + " (" + journalCount + "/" + elsevierJournals.size + ")")

    val journalPath = target.resolve(name)

    if (!Files.exists(journalPath)) {
      Files.createDirectory(journalPath)
      if (Files.exists(source.resolve("LICENSE.pdf"))) {
        Files.createSymbolicLink(journalPath.resolve("LICENSE.pdf"), source.resolve("LICENSE.pdf"))
      }
    }

    val files = scala.collection.mutable.Set[Path]() ++ pdfs(journalPath)

    val throttle = Throttle.linearBackoff(10000)

    var count = 0
    val articles = Journal(issn).openAccess.filterNot(a => missingButWhoCares_?.contains(a.identifierString)).toSeq

    println("Total articles: " + articles.size)
    println(articles.groupBy(_.year).mapValues(_.size).toSeq.sorted.mkString(" "))

    def incr {
      synchronized {
        count += 1
      }
    }

    for (group <- articles.grouped(100); article <- group.par; if article.journalOption != Some("Advancement in Math.")) {
      try {
        incr
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
                Logging.error("Exception while creating symbolic link for:\n" + article.bibtex.toBIBTEXString, e)
              }
            }
          } else {
            synchronized {
              files -= targetFile
            }
          }
        }
      } catch {
        case e: Exception => {
          Logging.error("Exception while checking: " + article.bibtex.toBIBTEXString, e)
        }
      }
    }

    println("Found " + count + " articles in " + name)
    for (file <- files) {
      println("Deleting: " + file.getFileName)
      Files.delete(file)
    }
  }
}