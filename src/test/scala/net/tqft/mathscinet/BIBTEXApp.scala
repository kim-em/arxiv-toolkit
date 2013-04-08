package net.tqft.mathscinet

import scala.collection.parallel.ForkJoinTaskSupport
import net.tqft.util.SeleniumSlurp

object BIBTEXApp extends App {
  SeleniumSlurp.disable

  val pool = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(100))

  for (group <- Articles.withCachedBIBTEX.grouped(10000); groupPar = { val p = group.par; p.tasksupport = pool; p }; article <- groupPar) {
    println(article.bibtex.toBIBTEXString)
  }
}