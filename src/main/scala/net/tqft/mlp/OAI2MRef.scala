package net.tqft.mlp

import scala.io.Source
import java.io.File
import net.tqft.mathscinet.MRef
import net.tqft.toolkit.collections.Split.splittableIterator
import net.tqft.wiki.WikiMap
import net.tqft.wiki.FirefoxDriver

object OAI2MRef extends App {


  lazy val arxivbot = {
    val b = WikiMap("http://tqft.net/mlp/index.php")
    b.login("arxivbot", "zytopex")
    b
  }

  import net.tqft.toolkit.collections.Split._

  var count = 0
  val input: File = new File("/Users/scott/projects/arxiv-toolkit/arxiv.txt")
  for (
    chunk <- Source.fromFile(input).getLines.splitOn(_.startsWith("---"));
    if chunk.nonEmpty;
    id = chunk(0).stripPrefix("id: ");
    journalRef = chunk(1).stripPrefix("jr: ");
    doi = chunk(2).stripPrefix("doi: ");
    title = chunk(3).stripPrefix("title: ");
    authors = chunk(4).stripPrefix("aa: ");
    if doi == "";
//    if (journalRef.contains("Discrete") || journalRef.contains("Adv") && journalRef.contains("Math") || journalRef.contains("Geom") && journalRef.contains("Fun") || journalRef.contains("Alg") && journalRef.contains("Geom") && journalRef.contains("Top"));
    if journalRef.contains("2013")
  ) {
    println(chunk)

    // TODO if the DOI is there, use it

    val citation = title + "\n" + authors + "\n" + journalRef
    val result = MRef.lookup(citation)
    println(result.map(_.identifierString))

    if (result.size == 1) {
      arxivbot("Data:" + result(0).identifierString + "/FreeURL") = "http://arxiv.org/abs/" + id
    }
    count += 1
  }
  println(count)

  FirefoxDriver.quit
}