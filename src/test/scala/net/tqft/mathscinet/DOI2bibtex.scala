package net.tqft.mathscinet

object DOI2bibtex extends App {
  //  val doi = "10.2140/pjm.2010.247.323"
  //  println(Article.fromDOI(doi).map(_.bibtex.toBIBTEXString))

  val hindawi = "10.1155"
  val wiley = "10.1002"
  val OUP = "10.1112"
  for (article <- Articles.withDOIPrefix(OUP + "/S")) {
    println(article.bibtex.toBIBTEXString)
  }
}