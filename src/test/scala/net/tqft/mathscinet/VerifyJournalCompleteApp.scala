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
import net.tqft.toolkit.amazon.S3
import org.apache.commons.io.IOUtils
import java.io.FileInputStream
import java.net.URL
import java.io.FileOutputStream

object VerifyJournalCompleteApp extends App {

  val missingButWhoCares_? = Seq("MR0337466", "MR0337465", "MR0337467", "MR1690573", "MR1690574") ++ // birthdays
    Seq("MR0585438", "MR0585435", "MR0973821") ++ // bad data on mathscinet?
    Seq("MR0949339", "MR1895524") ++ // memorials
    Seq("MR0532067", "MR0333021") // errata

  val missingFromElsevier = """
Inclusion relationships among permutation problems - Bovet, Daniel P. and Panconesi, Alessandro - Discrete Appl. Math. 16 (1987), no. 2, 113–123 - MR0874910
Vector systems of exponentials and zeroes of entire matrix-functions - Ivanov, S. A. and Pavlov, B. S. - J. Math. Anal. Appl. 74 (1980), no. 2, 25–31, 118 - MR0573400
Model-theoretic forcing in logic with a generalized quantifier - Bruce, Kim B. - Ann. Math. Logic 13 (1978), no. 3, 225–265 - MR0491860
""".split("\n").map(_.trim).filter(_.nonEmpty).map(_.split(" ").last).collect({ case MRIdentifier(id) => id })

  val usedToBeMissing = """A diametric theorem for edges - Ahlswede, R. and Khachatrian, L. H. - J. Combin. Theory Ser. A 92 (2000), no. 1, 1–16 - MR1783935
Longest paths in semicomplete multipartite digraphs - Volkmann, Lutz - Discrete Math. 199 (1999), no. 1-3, 279–284 - MR1675934
Necessary conditions for Hamiltonian split graphs - Peemöller, Jürgen - Discrete Math. 54 (1985), no. 1, 39–47 - MR0787491
Undecided Ramsey numbers for paths - Lindström, Bernt - Discrete Math. 43 (1983), no. 1, 111–112 - MR0680310
On the existence of cyclic and pseudo-cyclic MDS codes - Maruta, Tatsuya - European J. Combin. 19 (1998), no. 2, 159–174 - MR1607933
Characterization of antidominant solutions of linear differential equations - Hanschke, Thomas - J. Comput. Appl. Math. 40 (1992), no. 3, 351–361 - MR1170914
Dynamic programming of expectation and variance - Quelle, Gisbert - J. Math. Anal. Appl. 55 (1976), no. 1, 239–252 - MR0411651
On the completeness of the system of functions \{e^{\alpha(n+a)x}\sin(n+a)x\}^\infty_{n=0} - Dostanić, M. R. - J. Math. Anal. Appl. 203 (1996), no. 3, 672–676 - MR1417122
ABC candies - Ribenboim, Paulo - J. Number Theory 81 (2000), no. 1, 48–60 MR1743505
Metric methods three examples and a theorem - Fitting, Melvin - J. Logic Programming 21 (1994), no. 3, 113–127 - MR1300126
The stable models of a predicate logic program - Marek, V. Wiktor and Nerode, Anil and Remmel, Jeffrey B. - J. Logic Programming 21 (1994), no. 3, 129-154 - MR1300127
On the power of rule-based query languages for nested data models - Vadaparty, Kumar V. - J. Logic Programming 21 (1994), no. 3, 155–175 - MR1300128
Distributed logic programming - Brogi, Antonio and Gorrieri, Roberto - J. Logic Programming 15 (1993), no. 4, 295–335 - MR1208829
A completeness result for SLDNF-resolution - Stroetmann, Karl - J. Logic Programming 15 (1993), no. 4, 337–355 - MR1208830
On the existence of cyclic and pseudo-cyclic MDS codes - Maruta, Tatsuya - European J. Combin. 19 (1998), no. 2, 159–174 - MR1607933
Bounds on the number of fuzzy functions - Kameda, T. and Sadeh, E. - Information and Control 35 (1977), no. 2, 139–145 - MR0456965      
Performance of heuristic bin packing algorithms with segments of random length - Shapiro, Stephen D. - Information and Control 35 (1977), no. 2, 146–158 - MR0503867    
\aleph _{1}-trees - Devlin, Keith J. - Ann. Math. Logic 13 (1978), no. 3, 267–330 - MR0491861
On the choice of pencils in the parametrization of curves - Schicho, Josef - J. Symbolic Comput. 14 (1992), no. 6, 557–576 - MR1202372
The SetPlayer system for symbolic computation on power sets - Berque, David and Cecchini, Ronald and Goldberg, Mark and Rivenburgh, Reid - J. Symbolic Comput. 14 (1992), no. 6, 645–662 - MR1202375
A general refutational completeness result for an inference procedure based on associative-commutative unification - Paul, Etienne - J. Symbolic Comput. 14 (1992), no. 6, 577–618 - MR1202373
AC unification through order-sorted ACI unification - Domenjoud, Eric - J. Symbolic Comput. 14 (1992), no. 6, 537–556 - MR1202371
Computing the topology of a bounded nonalgebraic curve in the plane - Richardson, Daniel - J. Symbolic Comput. 14 (1992), no. 6, 619–643 - MR1202374
The composition factors of the principal indecomposable modules over the 0-Hecke algebra of type E_6 - Dong, Chen Cheng and Chun, Liu Jia and Jin, Qian - J. Algebra 245 (2001), no. 2, 695–718 - MR1863897
The existential theory of real hyperelliptic function fields - Zahidi, Karim - J. Algebra 233 (2000), no. 1, 65–86 - MR1793590
""".split("\n").map(_.trim).filter(_.nonEmpty).map(_.split(" ").last).collect({ case MRIdentifier(id) => id })

  val mysterious = """
  Some remarks on modular field extensions with subbases - Eke, B. I. - Theoret. Comput. Sci. 12 (1978), 100–108 - MR0603307
  For any admissible ordinal \alpha , Kripke’s \alpha -recursive functions are exactly Machover’s \alpha -recursive functions - Carpenter, Amos J. - Theoret. Comput. Sci. 12 (1978), 109–120 - MR0603308
  """.split("\n").map(_.trim).filter(_.nonEmpty).map(_.split(" ").last).collect({ case MRIdentifier(id) => id })

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

  val completedJournals = Seq(
    "Discrete Optim.",
    "J. Approx. Theory",
    "Topology Appl.",
    "Differential Geom. Appl.",
    "Finite Fields Appl.",
    "European J. Combin.",
    "Appl. Math. Model.",
    "Stochastic Process. Appl.",
    "Comput. Geom.",
    "Math. Comput. Modelling",
    "Adv. Math.",
    "Adv. in Appl. Math.",
    "J. Number Theory",
    "J. Math. Anal. Appl.",
    "J. Combin. Theory Ser. A",
    "Topology",
    "Appl. Comput. Harmon. Anal.",
    "Indag. Math. (N.S.)",
    "Comput. Math. Appl.",
    "J. Differential Equations",
    "J. Complexity",
    "Ann. Inst. H. Poincaré Anal. Non Linéaire",
    "Bull. Sci. Math.",
    "J. Discrete Algorithms",
    "J. Funct. Anal.",
    "Appl. Math. Lett.",
    "Inform. and Control",
    "J. Logic Programming",
    "J. Multivariate Anal.",
    "J. Combin. Theory Ser. B",
    "Exposition. Math.",
    "J. Comput. System Sci.",
    "Math. Modelling",
    "Historia Math.",
    "Discrete Appl. Math.",
    "J. Log. Algebr. Program.",
    "General Topology Appl.",
    "Discrete Math.",
    "J. Appl. Log.",
    "J. Pure Appl. Algebra",
    "Sci. Comput. Programming",
    "Ann. Pure Appl. Logic",
    "J. Math. Pures Appl.",
    "Internat. J. Approx. Reason.",
    "Ann. Math. Logic",
    "J. Symbolic Comput.",
    "Theoret. Comput. Sci.",
    "J. Comput. Appl. Math.",
    "Inform. and Comput.",
    "Linear Algebra Appl.",
    "J. Algebra")
  // TODO automatically upload them to S3 // hmm, some files are too big for j3tset.

  val readyToUpload = completedJournals.toSet -- Articles((missingFromElsevier ++ mysterious).map(_.toString)).map(_._2.journal)
  
    var journalCount = 0
  for ((issn, name) <- scala.util.Random.shuffle(elsevierJournals.toSeq); if !readyToUpload.contains(name)) {

    journalCount += 1
    println("Checking " + name + " " + issn + " (" + journalCount + "/" + (elsevierJournals.values.toSet -- completedJournals).size + ")")

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
      .filterNot(a => mysterious.contains(a.identifier))
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
  
  
  
  for (journal <- readyToUpload) {
    val tarFile = journal + " -2009.tar"
    val zipFile = tarFile + ".gz"
    import scala.sys.process._
    if (!new File(System.getProperty("user.home") + "/Literature/" + zipFile).exists) {
      if (!new File(System.getProperty("user.home") + "/Literature/" + tarFile).exists) {
        println("Tar'ing " + tarFile)
        val cmd = Seq("/opt/local/bin/gnutar", "--format=posix", "--dereference", "-cvf", tarFile, journal)
        println(cmd.mkString(" "))
        println(Process(cmd, Some(new File(System.getProperty("user.home") + "/Literature/"))).!!)
      }
      println("Gzipping ...")
      println(Process(Seq("/usr/bin/gzip", tarFile), Some(new File(System.getProperty("user.home") + "/Literature/"))).!!)
    }
    //    println("Uploading " + zipFile)
    //    S3.bytes("elsevier-open-math-archive").putIfAbsent(zipFile, IOUtils.toByteArray(new FileInputStream(System.getProperty("user.home") + "/Literature/" + zipFile)))
  }

  println("---wikitext---")
  println((for (journal <- readyToUpload) yield {
    val tarFile = journal + " -2009.tar"
    val zipFile = tarFile + ".gz"
    // https://s3.amazonaws.com/elsevier-open-math-archive/Ann.+Inst.+H.+Poincare%CC%81+Anal.+Non+Line%CC%81aire+-2009.tar.gz
    val torrent = "https://s3.amazonaws.com/elsevier-open-math-archive/" + zipFile.replaceAll(" ", "+").replaceAll("é", "e%CC%81") + "?torrent"
    "* [" + torrent + " " + journal + "]"
  }).toSeq.sorted.mkString("\n"))
  println("\n\nWe're waiting for a number of missing articles before publishing torrents for: \n" + Articles((missingFromElsevier ++ mysterious).map(_.toString)).map(_._2.journal).toSeq.distinct.sorted.mkString("* ", "\n* ", "\n"))
  println("------")

  for (journal <- readyToUpload) {
    val tarFile = journal + " -2009.tar"
    val zipFile = tarFile + ".gz"
    val torrent = "https://s3.amazonaws.com/elsevier-open-math-archive/" + zipFile.replaceAll(" ", "+").replaceAll("é", "e%CC%81") + "?torrent"
    val torrentFile = new File(System.getProperty("user.home") + "/Literature/torrents/" + zipFile + ".torrent")
    if (!torrentFile.exists) {
      println("Saving torrent: " + torrent)
      try {
        IOUtils.copy(new URL(torrent).openStream, new FileOutputStream(torrentFile))
      } catch {
        case e: Exception => println("Not found!")
      }
    }
  }


}