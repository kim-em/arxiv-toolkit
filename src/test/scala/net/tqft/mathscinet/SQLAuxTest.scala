//package net.tqft.mathscinet
//
//import net.tqft.mlp.sql.SQL
//import net.tqft.mlp.sql.SQLTables
//import scala.slick.driver.MySQLDriver.simple._
//import net.tqft.toolkit.Logging
//import net.tqft.util.pandoc
//import scala.collection.parallel.ForkJoinTaskSupport
//
//object SQLAuxTest extends App {
//
//  val a=Article(2615348)
//  
//  println("[" + a.bibtex.documentType + "]")
//  println(a.bibtex.get("MRCLASS"))
//  println(a.bibtex.toBIBTEXString)
//  println(a.bibtex.get("NOTE"))
//  
//  println(a.citation_text)
//
//}