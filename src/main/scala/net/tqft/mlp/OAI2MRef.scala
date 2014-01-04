package net.tqft.mlp

import scala.io.Source
import java.io.File
import net.tqft.mathscinet.MRef
import net.tqft.toolkit.collections.Split.splittableIterator
import net.tqft.wiki.WikiMap

object OAI2MRef extends App {

  import net.tqft.toolkit.collections.Split._

  lazy val arxivbot = {
    val b = WikiMap("http://tqft.net/mlp/index.php")
    b.login("arxivbot", "zytopex")
    b
  }

  val input: File = new File("/Users/scott/Downloads/sample.txt")
  for (chunk <- Source.fromFile(input).getLines.splitOn(_.startsWith("---")); if chunk.nonEmpty) {
    val id = chunk(0).stripPrefix("id: ")
    val journalRef = chunk(1).stripPrefix("jr: ")
    val doi = chunk(2).stripPrefix("doi: ")
    val title = chunk(3).stripPrefix("title: ")
    val authors = chunk(4).stripPrefix("aa: ")

    // TODO if the DOI is there, use it

    val citation = title + "\n" + authors + "\n" + journalRef
    val result = MRef.lookup(citation)
    println(result.map(_.identifierString))

    if (result.size == 1) {
    	arxivbot("Data:" + result(0).identifierString + "/FreeURL") = "http://arxiv.org/abs/" + id
    }
  }

}