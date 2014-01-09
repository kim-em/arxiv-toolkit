package net.tqft.util

import java.io.InputStream
import java.io.BufferedInputStream
import eu.medsea.mimeutil.MimeUtil
import net.tqft.toolkit.Logging
import org.apache.commons.io.IOUtils

object PDF {
  def getBytes(url: String): Option[Array[Byte]] = {
    getBytes(HttpClientSlurp.getStream(url))
  }
  def getBytes(pdfInputStream: InputStream): Option[Array[Byte]] = {
    val bis = new BufferedInputStream(pdfInputStream)
    bis.mark(20)
    val prefix = new Array[Byte](10)
    bis.read(prefix)
    bis.reset
    val prefixString = new String(prefix)
    val mimetype = if (prefixString.contains("%PDF")) {
      "application/pdf"
    } else {
      MimeUtil.getMimeTypes(bis).toString
    }
    mimetype match {
      case "application/pdf" => {
        Logging.info("Obtained bytes for PDF.")
        val result = Some(IOUtils.toByteArray(bis))
        bis.close
        result
      }
      case t => {
        Logging.warn("Content does not appear to be a PDF! (File begins with " + prefixString + " and MIME type detected as " + t + ".)")
//        Logging.warn(IOUtils.toString(bis))
        bis.close
        None
      }
    }
  }
}