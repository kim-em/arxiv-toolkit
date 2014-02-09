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

    val missingFromElsevier = """
A diametric theorem for edges - Ahlswede, R. and Khachatrian, L. H. - J. Combin. Theory Ser. A 92 (2000), no. 1, 1–16 - MR1783935
Longest paths in semicomplete multipartite digraphs - Volkmann, Lutz - Discrete Math. 199 (1999), no. 1-3, 279–284 - MR1675934
Necessary conditions for Hamiltonian split graphs - Peemöller, Jürgen - Discrete Math. 54 (1985), no. 1, 39–47 - MR0787491
Undecided Ramsey numbers for paths - Lindström, Bernt - Discrete Math. 43 (1983), no. 1, 111–112 - MR0680310
On the existence of cyclic and pseudo-cyclic MDS codes - Maruta, Tatsuya - European J. Combin. 19 (1998), no. 2, 159–174 - MR1607933
Characterization of antidominant solutions of linear differential equations - Hanschke, Thomas - J. Comput. Appl. Math. 40 (1992), no. 3, 351–361 - MR1170914
Inclusion relationships among permutation problems - Bovet, Daniel P. and Panconesi, Alessandro - Discrete Appl. Math. 16 (1987), no. 2, 113–123 - MR0874910
Dynamic programming of expectation and variance - Quelle, Gisbert - J. Math. Anal. Appl. 55 (1976), no. 1, 239–252 - MR0411651
On the completeness of the system of functions \{e^{\alpha(n+a)x}\sin(n+a)x\}^\infty_{n=0} - Dostanić, M. R. - J. Math. Anal. Appl. 203 (1996), no. 3, 672–676 - MR1417122
Vector systems of exponentials and zeroes of entire matrix-functions - Ivanov, S. A. and Pavlov, B. S. - J. Math. Anal. Appl. 74 (1980), no. 2, 25–31, 118 - MR0573400
ABC candies - Ribenboim, Paulo - J. Number Theory 81 (2000), no. 1, 48–60 MR1743505
Metric methods three examples and a theorem - Fitting, Melvin - J. Logic Programming 21 (1994), no. 3, 113–127 - MR1300126
The stable models of a predicate logic program - Marek, V. Wiktor and Nerode, Anil and Remmel, Jeffrey B. - J. Logic Programming 21 (1994), no. 3, 129-154 - MR1300127
On the power of rule-based query languages for nested data models - Vadaparty, Kumar V. - J. Logic Programming 21 (1994), no. 3, 155–175 - MR1300128
Distributed logic programming - Brogi, Antonio and Gorrieri, Roberto - J. Logic Programming 15 (1993), no. 4, 295–335 - MR1208829
A completeness result for SLDNF-resolution - Stroetmann, Karl - J. Logic Programming 15 (1993), no. 4, 337–355 - MR1208830
On the existence of cyclic and pseudo-cyclic MDS codes - Maruta, Tatsuya - European J. Combin. 19 (1998), no. 2, 159–174 - MR1607933
Model-theoretic forcing in logic with a generalized quantifier - Bruce, Kim B. - Ann. Math. Logic 13 (1978), no. 3, 225–265 - MR0491860
""".split("\n").filter(_.nonEmpty).map(_.split(" ").last).collect({ case MRIdentifier(id) => id })
    
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
    val articles = Journal(issn).openAccess
    .filterNot(a => missingButWhoCares_?.contains(a.identifierString))
    .filterNot(a => missingFromElsevier.contains(a.identifier))
    .toSeq

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