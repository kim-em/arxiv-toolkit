package net.tqft.mathscinet

import scala.collection.parallel.ForkJoinTaskSupport
import net.tqft.util.FirefoxSlurp
import net.tqft.journals.ISSNs
import net.tqft.toolkit.amazon.S3
import net.tqft.util.BIBTEX
import net.tqft.toolkit.amazon.AnonymousS3
import java.io.File
import java.io.PrintStream
import java.io.FileOutputStream

object BIBTEXApp extends App {
  //  FirefoxSlurp.disable

  val pool = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(100))

  val cache = AnonymousS3("LoM-bibtex")
  //  for(group <- cache.keysIterator.grouped(1000); groupPar = { val p = group.par; p.tasksupport = pool; p }; k <- groupPar; if k.length < 9) {
  //    println("Found short identifier: " + k)
  //    val l = "MR" + ("0000000" + k.drop(2)).takeRight(7)
  //    cache.putIfAbsent(l, BIBTEX.parse(cache(k)).get.toBIBTEXString)
  //    cache -= k
  //  }

  val repository = new File(System.getProperty("user.home") + "/Literature/mathscinet-data")
  val dir = new File(repository, "bibtex")
  //  for (group <- Articles.withCachedBIBTEX.grouped(10000); groupPar = { val p = group.par; p.tasksupport = pool; p }; article <- groupPar) {
  for (article <- Articles.withCachedBIBTEX) {
    try {
      val file = new File(dir, article.identifierString + ".bib")
      if (!file.exists) {
        println("Saving " + article.identifierString)
        new PrintStream(new FileOutputStream(file)).println(article.bibtex.toBIBTEXString)
      }
    } catch {
      case e: Exception => println(e)
    }
  }
}