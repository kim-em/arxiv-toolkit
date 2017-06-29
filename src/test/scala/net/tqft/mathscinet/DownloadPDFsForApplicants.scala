package net.tqft.mathscinet

import java.io.File
import com.github.tototoshi.csv.CSVReader

object DownloadPDFForApplicants extends App {
  val reader = CSVReader.open(new File("/Users/scott/scratch/female-only/Summary.csv"))
  val data = for (
    Seq(first, last, url) <- reader.all.tail.map(row =>
      Seq(row(0), row(1), row(7)));
    if url.contains("&s1=");
    id = url.split("&s1=")(1).split("&")(0).toInt
  ) yield (first, last, id)

  //  data.map( println _ )

  for ((first, last, id) <- data) {
    println(first + " " + last)
    val dir = "/Users/scott/scratch/female-only/" + last + ", " + first + "/papers"
    new File(dir).mkdirs()
    for (a <- Author(id, first + " " + last).articles) {
      try {
        println(a.sanitizedTitle)
        a.savePDF(dir)
      } catch {
        case e: Exception => 
      }
    }
  }
}