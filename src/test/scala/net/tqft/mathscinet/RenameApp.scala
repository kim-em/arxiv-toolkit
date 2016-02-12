package net.tqft.mathscinet

import java.io.File
import net.tqft.toolkit.Extractors._

object RenameApp extends App {
  val base = args.mkString(" ")

  import scala.collection.JavaConversions._

  def getFileTree(f: File): Stream[File] =
    f #:: (if (f.isDirectory) f.listFiles().toStream.flatMap(getFileTree)
    else Stream.empty)

  val r = """MR([0-9]*)\.pdf""".r
  for (file <- getFileTree(new File(base)); if file.getName.endsWith(".pdf")) {
    for(m <- r.findFirstMatchIn(file.getName); Int(id) = m.group(1)) {
    println(id)
    println(file.getParentFile.getAbsolutePath)
    Article(id).savePDF(file.getParentFile.getAbsolutePath)
  }
  }
}