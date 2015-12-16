package net.tqft.kovacs

import net.tqft.toolkit.collections.Split._
import scala.io.Source
import net.tqft.mathscinet.Article
import net.tqft.util.pandoc
import java.io.File
import scalaz._
import Scalaz._
import argonaut._
import Argonaut._
import java.io.FileOutputStream
import java.io.PrintWriter
import net.tqft.util.HTMLEncode
import net.tqft.mathscinet.Search

/**
 * @author scott
 */
object ParsePubs extends App {
  val root = new File("/Users/scott/Dropbox/LGK/")

  val pubs = new File(root, "Pubs.json")
  val html = new File(root, "Pubs.html")

  val json = if (pubs.exists) {
    Source.fromFile(pubs).getLines.mkString("\n").decode[List[Map[String, String]]].getOrElse(Nil).asJson
  } else {

    val lines = Source.fromFile("/Users/scott/Dropbox/LGK/Pubs.tex").getLines.toStream

    val refs = lines.splitOn(_.isEmpty).filter(_.head.trim == "\\ref").map({ ref =>
      (ref.mkString("\n").split("\n\\\\").map(_.split("\\s").toList match {
        case tag :: remainder => tag.replaceAll("nofrills\\\\", "") -> remainder.mkString(" ").replaceAll("\n", " ").trim
        case Nil => ???
      })).toMap - "" - "\\ref" - "endref" - "nofrills"
    })

    val ref2 = (for (ref <- refs) yield {
      val matches = net.tqft.citationsearch.Search.goodMatch(ref.get("paper").getOrElse(ref("book")) + " Kovacs " + ref.get("by").getOrElse("").replaceAll("with", "") + " " + ref.get("jour").getOrElse("") + " " + ref.get("year").getOrElse("") + " " + ref.get("pages").getOrElse("") + " " + ref.get("vol").getOrElse("")).map(_.citation)

      val mr = ref("no") match {
        case "16"=> Some(215897)
        case "47" => None
        case "53" => None
        case "77" => Some(1092217)
        case "80" => Some(1119007)
        case "84" => None
        case "87" => None
        case _ => matches.flatMap(_.MRNumber)
      }
        
      val doi = ref("no") match {
        case "48" => Some("10.1017/S1446788700024770")
        case "98" => Some("10.1017/S1446788700001130")
        case _ => mr.flatMap(i => Article(i).DOI)
      }
        
      val extra = (ref ++ mr.map(id => "mrnumber" -> ("MR" + id.toString)).toSeq
        ++ matches.flatMap(_.arXiv.map(id => "arxiv" -> id)).toSeq
        ++ doi.map(d => "doi" -> d).toSeq
        ++ ref.get("by").map(by => "by" -> pandoc.latexToText(by))
        ++ ref.get("finalinfo").map(finalinfo => "finalinfo" -> pandoc.latexToText(finalinfo))
        ++ ref.get("inbook").map(inbook => "inbook" -> pandoc.latexToText(inbook))
        ++ ref.get("paper").map(paper => "paper" -> pandoc.latexToText(paper))
        ++ ref.get("pages").map(pages => "pages" -> pages.stripSuffix(";"))).toMap

      println(extra)
      extra
    })
    ref2.toList.asJson
  }

  println(json.spaces4)

  val pw = new PrintWriter(new FileOutputStream(pubs))
  pw.println(json.spaces4)
  pw.close

  val papers = json.spaces4.decode[List[Map[String, String]]].getOrElse(Nil)

  val sb = new StringBuilder
  
  sb ++= "<!-- start of automatically generated section: please do not edit by hand -->\n"

  def MRLink(paper: Map[String, String]) = {
    paper.get("mrnumber") match {
      case None => ""
      case Some(id) => s"""<a href="https://www.ams.org/mathscinet-getitem?mr=${id.stripPrefix("MR")}" class="mr">${id}</a>"""
    }
  }
  def DOILink(paper: Map[String, String]) = {
    paper.get("doi") match {
      case None => ""
      case Some(doi) => s"""<a href="https://dx.doi.org/$doi" class="doi">DOI:$doi</a>"""
    }
  }

  def enc(s: String) = {
    HTMLEncode.encode(s.replaceAllLiterally("\\&", "&").replaceAllLiterally("~", " "))
  }

  def pdfLink(paper: Map[String, String]) = {
    val title = enc(paper.getOrElse("paper", paper("book")))
    val filename = s"K${("000" + paper("no")).takeRight(3)}.pdf"
    if (new File(root, filename).exists) {
      s"""<a href="$filename" class="pdf"><span class="title"><i>$title</i></span></a>"""
    } else {
      s"""<span class="title"><i>$title</i></span>"""
    }
  }

  def yr(p: Map[String, String]) = {
    try {
      p("yr").toInt
    } catch {
      case e: Exception => {
        println(p)
        throw e
      }
    }
  }

  val decades = papers.groupWhen({ case (a, b) => yr(a) / 10 == yr(b) / 10 })
  
  for (papers <- decades) {
    val decade = ((yr(papers.head) / 10) * 10).toString + "s"

    sb ++= s"""<div class="timeline" id="$decade">$decade</div>\n"""
    sb ++= s"""<dl class="collected-works">\n"""

    for (paper <- papers) {

      try {
        sb ++= " <dt>\n"
        sb ++= s"""  ${paper("no")}. ${pdfLink(paper)}\n"""
        sb ++= " </dt>\n"
        sb ++= " <dd>\n"

        def line(s: String) = {
          if (s.trim.nonEmpty) {
            sb ++= "  " + s + "<br/>\n"
          }
        }
        def x(a: String) = {
          enc(paper.getOrElse(a, ""))
        }
        def y(a: String, before: String, after: String) = {
          paper.get(a).map(enc) match {
            case Some(t) => before + t.stripPrefix(before).stripSuffix(after) + after
            case None => ""
          }
        }

        line(s"${x("by")}")
        line(s"${x("jour")} ${y("vol", "<b>", "</b>")} ${x("inbook")} ${x("publ")} ${x("publaddr")} ${y("yr", "(", ")")} ${y("pages", "pp. ", "")}")
        line(y("finalinfo", "(", ")"))
        line(s"${MRLink(paper)} ${DOILink(paper)}")
        sb ++= " </dd>\n"
      } catch {
        case e: Exception => {
          println(e)
          println(paper)
          throw e
        }
      }
    }

    sb ++= "</dl>\n"
  }

    sb ++= "<!-- end of automatically generated section: please do not edit by hand --->\n"

  
  val html_pw = new PrintWriter(new FileOutputStream(html))
  html_pw.println(sb)
  html_pw.close

  //  println(sb)

//      val missingMRNumbers = Search.byAuthorIdentifier(105700).map(_.identifier).toSet -- papers.flatMap(_.get("mrnumber")).map(_.stripPrefix("MR").toInt)
//    for(mr <- missingMRNumbers) {
//      println(Article(mr))
//    }

}