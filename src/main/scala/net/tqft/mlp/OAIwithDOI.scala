package net.tqft.mlp

import net.tqft.wiki.WikiMap
import java.io.File
import scala.io.Source
import net.tqft.mathscinet.Article

object OAIwithDOI extends App {

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
    doi = chunk(2).stripPrefix("doi: ");
    if doi.nonEmpty;
    id = chunk(0).stripPrefix("id: ");
    journalRef = chunk(1).stripPrefix("jr: ");
    title = chunk(3).stripPrefix("title: ");
    authors = chunk(4).stripPrefix("aa: ")
//    if journalRef.contains("2013")
  ) {
    for (article <- Article.fromDOI(doi)) {
      count += 1
      arxivbot("Data:" + article.identifierString + "/FreeURL") = "http://arxiv.org/abs/" + id
    }
    println(count)
  }

}