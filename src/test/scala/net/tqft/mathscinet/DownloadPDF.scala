package net.tqft.mathscinet

object DownloadPDF extends App {
  for(a <- args) {
    val article = Article(a)
    article.savePDF("/Users/scott/Downloads/")
  }
}