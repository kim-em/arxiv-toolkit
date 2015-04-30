package net.tqft.webofscience

object SearchForDOIApp extends App {
  println(Article.searchForDOI("10.1093/gji/ggu038"))
  
  FirefoxDriver.quit
}