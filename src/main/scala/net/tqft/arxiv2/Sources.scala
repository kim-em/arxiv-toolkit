package net.tqft.arxiv2

import net.tqft.util.PDF
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import scala.collection.mutable.ListBuffer
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.io.IOUtils
import java.io.File
import java.nio.file.Paths
import java.nio.file.Files
import scala.sys.process.ProcessBuilder.Source
import scala.io.Source
import net.tqft.citationsearch.CitationScore
import net.tqft.util.pandoc
import java.io.IOException

trait Sources {
  def rawSourceFile(identifier: String): Option[Array[Byte]]
  def apply(identifier: String): Map[String, Array[Byte]] = {
    rawSourceFile(identifier) match {
      case None => Map.empty
      case Some(bytes) => PDF.getBytes(new ByteArrayInputStream(bytes)) match {
        case Some(pdfBytes) => Map(identifier + ".pdf" -> pdfBytes)
        case None => {
          try {
            val tis = new TarArchiveInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes)))
            var entry: ArchiveEntry = null
            val buffer = ListBuffer[(String, Array[Byte])]()
            while ({ entry = tis.getNextEntry; entry != null }) {
              if (!entry.isDirectory) {
                buffer += ((entry.getName, IOUtils.toByteArray(tis)))
              }
            }
            buffer.toMap
          } catch {
            case e: IOException => {
              require(e.getMessage == "Error detected parsing the header")
              Map(identifier + ".tex" -> IOUtils.toByteArray(new GZIPInputStream(new ByteArrayInputStream(bytes))))
            }
          }
        }
      }
    }
  }
  def bibitems(identifier: String): Seq[(String, String)] = {
    val lines = apply(identifier).collect({ case (name, bytes) if name.toLowerCase.endsWith(".tex") || name.toLowerCase.endsWith(".bbl") => Source.fromBytes(bytes).getLines }).iterator.flatten
    var state = false
    val bibitemLines = lines.filter({ line =>
      // TODO allow whitespace after 'begin' and 'end'
      if (line.contains("\\begin{thebibliography}")) {
        state = true
        false
      } else if (line.contains("\\end{thebibliography}")) {
        state = false
        false
      } else {
        state
      }
    })
    val buffer = ListBuffer[(String, String)]()
    var key: String = null
    val entryBuffer = ListBuffer[String]()
    def post {
      if (key != null) {
        buffer += ((key, entryBuffer.filter(_.trim.nonEmpty).mkString("\n")))
      }
      entryBuffer.clear
    }
    for (line <- bibitemLines) {
      if (line.startsWith("\\bibitem")) {
        post
        
        val tail = line.stripPrefix("\\bibitem").trim
        
        val (key_, remainder) = {
          if(tail.startsWith("{")) {
            (
            		line.stripPrefix("{").takeWhile(_ != '}'),
            		line.stripPrefix("{").dropWhile(_ != '}').stripPrefix("}")
            )
          } else if(tail.startsWith("[")) {
            (
                line.stripPrefix("[").takeWhile(_ != ']').stripPrefix("{").stripSuffix("}"),
                line.stripPrefix("[").dropWhile(_ != ']').stripPrefix("[").dropWhile(_ != '}').stripPrefix("}")
            )
          } else {
            println("I don't know how to parse this bibitem line:")
            println(line)
            ???
          }
        }
        
        key = key_
        entryBuffer += remainder
      } else {
        entryBuffer += line
      }
    }
    post
    buffer.toSeq
  }

  def referencesResolved(identifier: String) = bibitems(identifier).par.map({
    case (key, text) =>
      (key, text, net.tqft.citationsearch.Search.goodMatch(pandoc.latexToText(text)))
  }).seq

  def referencesResolvedToMathSciNet(identifier: String) = referencesResolved(identifier).par.map(t => (t._1, t._2, t._3.flatMap(_.citation.MRNumber).map(net.tqft.mathscinet.Article.apply))).seq
}

trait SourcesFromLocalCopy extends Sources {
  def basePath: String
  override def rawSourceFile(identifier: String) = {
    val path = Paths.get(basePath).resolve(identifier.split("/").last.take(4))
    val stub = identifier.replaceAllLiterally("/", "")
    val gz = path.resolve(stub + ".gz")
    val pdf = path.resolve(stub + ".pdf")
    if (Files.exists(gz)) {
      Some(IOUtils.toByteArray(gz.toUri()))
    } else if (Files.exists(pdf)) {
      Some(IOUtils.toByteArray(pdf.toUri()))
    } else {
      None
    }
  }
}

object Sources extends SourcesFromLocalCopy {
  override val basePath = "/Users/scott/scratch/arxiv/"
}
