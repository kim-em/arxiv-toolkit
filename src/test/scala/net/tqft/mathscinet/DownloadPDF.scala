package net.tqft.mathscinet

import net.tqft.toolkit.Extractors._

object DownloadPDFsBy extends App {
  for(Int(authorId) <- args) {
    val author = Author(authorId, "")
    for(article <- author.articles) {
      article.savePDF("/Users/scott/Literature/Downloads/")
    }
  }
}