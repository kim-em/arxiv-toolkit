package net.tqft.util

import java.io.InputStream
import org.apache.pdfbox.util.PDFTextStripper
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdfparser.PDFParser
import java.net.URL

object TokenizePDF {
  def apply(i: InputStream): Map[String, Int] = {
    import net.tqft.toolkit.collections.Tally._
    val parser = new PDFParser(i)
    parser.parse
    val text = new PDFTextStripper().getText(new PDDocument(parser.getDocument))
    text.split(" ").groupBy(x => x).mapValues(_.size)
  }

  def apply(url: URL): Map[String, Int] = apply(url.openStream())
}