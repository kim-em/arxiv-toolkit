package net.tqft.mathscinet

import scala.collection.parallel.ForkJoinTaskSupport
import net.tqft.util.FirefoxSlurp
import net.tqft.journals.ISSNs

object BIBTEXApp extends App {
  FirefoxSlurp.disable

  val pool = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(100))

  for (group <- Articles.withCachedBIBTEX.grouped(10000); groupPar = { val p = group.par; p.tasksupport = pool; p }; article <- groupPar; if article.ISSNOption == Some(ISSNs.`K-Theory`)) {
    println(article.bibtex.toBIBTEXString)
  }
}