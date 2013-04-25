package net.tqft.mathscinet

import net.tqft.toolkit.amazon.AnonymousS3
import scala.collection.parallel.ForkJoinTaskSupport
import net.tqft.toolkit.amazon.S3

object SmallCachedPagesApp extends App {

  val pool = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(50))

  val bucket = AnonymousS3("www.ams.org.cache")
  for (group <- bucket.keysIterator.grouped(500)) {
    for (k <- { val p = group.par; p.tasksupport = pool; p }; length <- bucket.contentLength(k)) {
      if (length < 1000) {
        println(k + " has length " + length)
        val text = bucket(k)
        if(text.contains("<title>500 Internal Server Error</title>")) {
          println("deleting error page...")
          S3("www.ams.org.cache") -= k
        }
        println(text)
      }
    }
  }
}