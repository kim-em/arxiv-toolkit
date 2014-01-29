package net.tqft.mathscinet

import java.io.File
import java.io.FilenameFilter
import net.tqft.util.FirefoxSlurp
import net.tqft.journals.ISSNs
import java.text.Normalizer
import java.io.PrintStream
import java.io.FileOutputStream
import scala.collection.parallel.ForkJoinTaskSupport
import java.text.SimpleDateFormat
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.DirectoryStream
import java.nio.file.Path
import net.tqft.journals.Journals

object CanonicalizePDFNamesApp extends App {
  //    FirefoxSlurp.disable
  //  Article.disableBibtexSaving

  val directory = new File("/Volumes/Repository Backups/elsevier-oa/")
  val otherDirectory = new File("/Volumes/Repository Backups-1/elsevier-oa/")
  val ktdirectory = new File("/Volumes/Repository Backups/k-theory/")
  val dropboxDirectory = new File(System.getProperty("user.home") + "/Dropbox/Apps/Papers from mathscinet")

  val identifierRegex = "MR[0-9]{7}".r

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

  private var renameCounter = 0
  def renameScript(directory: File) = {
    val name =
      synchronized {
        renameCounter = renameCounter + 1
        "doRename" + renameCounter.toString
      }
    val r = new File(directory, name)
    val ps = new PrintStream(new FileOutputStream(r))
    ps.println("")
    ps.close()
    scala.sys.process.Process(Seq("chmod", "u+x", name), directory).!
    r
  }

  def safeRename(identifier: String, directory: File, name: String) {
    val script = renameScript(directory)
    val ps = new PrintStream(new FileOutputStream(script))
    ps.println("mv *" + identifier + ".pdf " + "\"" + name + "\"")
    ps.close()
    scala.sys.process.Process(Seq("./" + script.getName), directory).!
    script.delete()
  }

  def pdfs(directory: Path) = {
    import scala.collection.JavaConverters._

    Files.newDirectoryStream(directory, new DirectoryStream.Filter[Path] {
      override def accept(path: Path) = {
        identifierRegex.findFirstMatchIn(path.getFileName.toString).nonEmpty
      }
    }).iterator.asScala
  }

  val pool = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(100))

  val formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  //  val directories = Seq(dropboxDirectory, otherDirectory, ktdirectory, directory)
  val directories = Seq(directory)

  val literature = Paths.get(new File(System.getProperty("user.home") + "/Literature").toURI())

  for (
    dir <- directories.filter(_.exists);
    dirPath = Paths.get(dir.toURI());
    group <- pdfs(dirPath).grouped(1000);
    identifiers = group.map(file => file -> identifierRegex.findAllMatchIn(file.getFileName.toString).toSeq.lastOption.map(_.matched));
    articles = Articles(identifiers.flatMap(_._2));
    (file, Some(identifier)) <- identifiers.par;
    article = articles.get(identifier).getOrElse(Article(identifier))
  ) {
    import java.nio.file.StandardCopyOption._

//    println(article.bibtex.toBIBTEXString)
    println(identifier)
    val newName = article.constructFilename()
    if (file.toFile.getName != newName) {
      println(formatter.format(new java.util.Date()))
      println("Renaming:\n  " + file.getFileName + "\nto\n  " + newName)
      println("Original title:\n  " + article.title)
      Files.move(file, file.resolveSibling(newName), REPLACE_EXISTING)
    }

    // now create a symbolic link
    val journalDir = literature.resolve(Journals.names(article.ISSN))
    if (!Files.exists(journalDir)) {
      Files.createDirectory(journalDir)
      if (ISSNs.Elsevier.contains(article.ISSN)) {
        if (Files.exists(dirPath.resolve("LICENSE.pdf"))) {
          Files.copy(dirPath.resolve("LICENSE.pdf"), journalDir.resolve("LICENSE.pdf"))
        }
      }
    }
    val link = journalDir.resolve(newName)
    if(!Files.exists(link)) {
      Files.createSymbolicLink(link, file.resolveSibling(newName))
    }
  }

}