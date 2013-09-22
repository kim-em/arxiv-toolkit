package net.tqft.mathscinet

import java.io.File
import net.tqft.util.CSVParser
import net.tqft.toolkit.Extractors.Int
import scala.io.Codec

object CitationApp extends App {

  Article.disableBibtexSaving
  
  val mathematicians = (for(line <- io.Source.fromFile(new File(System.getProperty("user.home") + "/projects/arxiv-toolkit/mathematicians.txt"))(Codec.UTF8).getLines;
		  					if line.nonEmpty && !line.startsWith("#");
		  					fields = CSVParser(line)
  ) yield fields).toList
    
  val authors = for (Int(id) :: name :: university :: level :: _  <- mathematicians;
		  		if id > 0)  yield {
	  Author(id, name)
  }
    
  for(a <- authors) println(a.name + " " + a.hIndex())
}