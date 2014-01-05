package net.tqft.util

import java.net.URL

object ComparePDFApp extends App {

  val urls = List("http://arxiv.org/pdf/1203.4419", "http://arxiv.org/pdf/1203.4919").map(new URL(_))
  val tokens = urls.map(TokenizePDF(_))
  
  println(tokens.map(_.values.sum))
  
  val m = for(t1 <- tokens) yield {
    for(t2 <- tokens) yield {
      Angle(t1, t2)
    }
  }
  
  println(m)
}