package net.tqft.mathscinet

import net.tqft.toolkit.amazon.AnonymousS3
import scala.collection.parallel.ForkJoinTaskSupport
import net.tqft.toolkit.amazon.S3

object Deleting2015CachesApp extends App {

  val pool = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(50))

  val bucket = AnonymousS3("www.ams.org.cache")
  for (group <- bucket.keysIterator.grouped(500)) {
    for (k <- { val p = group.par; p.tasksupport = pool; p }; if k.contains("=2015&")) {
          println("deleting 2015 cached page... " + k)
          S3("www.ams.org.cache") -= k
    }
  }
}