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

object CanonicalizePDFNamesApp extends App {
  //    FirefoxSlurp.disable
  //  Article.disableBibtexSaving

  val directory = new File("/Volumes/Repository Backups/elsevier-oa/")
  val otherDirectory = new File("/Volumes/Repository Backups-1/elsevier-oa/")
  val ktdirectory = new File("/Volumes/Repository Backups/k-theory/")
  val dropboxDirectory = new File(System.getProperty("user.home") + "/Dropbox/Apps/Papers from mathscinet")

  val identifierRegex = "MR[0-9]{7}".r

  val articles = (for (
    a <- Articles.fromBibtexFile(System.getProperty("user.home") + "/projects/arxiv-toolkit/100_4.bib") //;
  //    issn <- a.ISSNOption;
  //    if ISSNs.Elsevier.contains(issn)
  ) yield { a.identifierString -> a }).toMap

  //  val articles = Map.empty[String, Article]

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

  def pdfs(directory: File) = {
    scala.sys.process.Process("ls", directory).lines_!.iterator.filter(name => identifierRegex.findFirstMatchIn(name).nonEmpty).map(name => new File(directory, name))
  }

  // If we've accidentally mangled file names, .listFiles might actually unmangle them for us, hiding the problem. Hence this version is no good!
  //  def pdfs(directory: File) = {
  //    directory.listFiles(new FilenameFilter { override def accept(dir: File, name: String) = name.contains("probabilistic") && identifierRegex.findFirstMatchIn(name).nonEmpty })
  //  }

  val pool = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(100))

  val formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  for (
    dir <- Seq(dropboxDirectory, otherDirectory, ktdirectory, directory).filter(_.exists);
    //    group <- pdfs(dir).grouped(1000);
    //    file <- { val p = group.par; p.tasksupport = pool; p };
    file <- pdfs(dir);
    //    if file.getName().contains(" - Adv.");
    identifier <- identifierRegex.findAllMatchIn(file.getName()).toSeq.lastOption.map(_.matched);
    article = articles.get(identifier).getOrElse(Article(identifier))
  ) {
    val newName = article.constructFilename()
    if (file.getName != newName) {
      println(formatter.format(new java.util.Date()))
      println("Renaming " + file.getName + " to " + newName)
      println("Original title: " + article.title)
      safeRename(identifier, dir, newName)
    }
  }

}