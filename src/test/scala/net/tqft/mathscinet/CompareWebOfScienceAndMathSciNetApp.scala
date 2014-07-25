package net.tqft.mathscinet

import java.io.File
import scala.io.Codec
import net.tqft.util.CSVParser
import net.tqft.toolkit.Extractors.Int
import net.tqft.toolkit.Extractors.Long
import net.tqft.citationsearch.CitationScore
import net.tqft.util.FirefoxSlurp
import java.io.PrintWriter
import java.io.FileOutputStream
import net.tqft.citationsearch.Citation
import java.io.OutputStreamWriter

object CompareWebOfScienceAndMathSciNetApp extends App {

  val outputFile = new File(System.getProperty("user.home") + "/projects/arxiv-toolkit/compare-WOS.html")
  outputFile.delete
  val out = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8")
  def p(s: String) = {
    println(s)
    out.write(s + "\n")
    out.flush
  }
  val missing = new File(System.getProperty("user.home") + "/projects/arxiv-toolkit/missing-from-webofscience")
  missing.delete
  val missing_out = new OutputStreamWriter(new FileOutputStream(missing), "UTF-8")
  def reportMissing(author: Int, target: String, source: String) = {
    missing_out.write(author + "\n")
    missing_out.write(target + "\n")
    missing_out.write(source + "\n")
    missing_out.write("\n")
    missing_out.flush
  }
 

  val mathematicians = (for (
    line <- io.Source.fromFile(new File(System.getProperty("user.home") + "/projects/arxiv-toolkit/mathematicians.txt"))(Codec.UTF8).getLines;
    if line.nonEmpty && !line.startsWith("#");
    fields = CSVParser(line)
  ) yield fields).toList

  val authors = for (
    Int(mathscinetAuthorId) :: _ :: name :: "ANU" :: level :: _ <- mathematicians; if mathscinetAuthorId > 0
  ) yield {
    Author(mathscinetAuthorId, name)
  }

  val firstYear = 2005

  def fullCitation_html(c: Citation) = {
    s"${c.title} - ${c.authors} - ${c.citation_html} ${c.MRNumber.map(n => "- <a href='http://www.ams.org/mathscinet-getitem?mr=" + n + "'>MR" + n + "</a>").getOrElse("")}"
  }

  p("""<!DOCTYPE html>
<html lang="en">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
<script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.6.4/jquery.min.js"></script>
<script type="text/x-mathjax-config">
        MathJax.Hub.Config(
            {
                "HTML-CSS": { preferredFont: "TeX", availableFonts: ["STIX","TeX"] },
                tex2jax: {
                    inlineMath: [ ["$", "$"], ["\\\\(","\\\\)"] ],
                    displayMath: [ ["$$","$$"], ["\\[", "\\]"] ],
                    processEscapes: true,
                    ignoreClass: "tex2jax_ignore|dno"
                },
                TeX: {
                    noUndefined: { attributes: { mathcolor: "red", mathbackground: "#FFEEEE", mathsize: "90%" } }
                },
                messageStyle: "none"
            });
</script>    
<script type="text/javascript" src="http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS_HTML"></script>
<script type="text/css">
      table {
		  border-collapse: collapse;
	  }
      table, th, td {
		  border: 1px solid black;     
	  }
      th {
           width: 50%; 
      }
</script>
<head>
<body>""")

  var unmatchedCount = 0

  for (author <- authors) {
    p("""<h2><a onclick="$('#publications-""" + author.id + s"""').toggle('fast')">Publications for <i>${author.name}</i> since $firstYear</a></h2>""")
    p(s"<div id='publications-${author.id}' style='display: none'>")

    lazy val recentPublicationsOnMathSciNet = author.articles.filter(a => a.yearOption.nonEmpty && a.yearOption.get >= firstYear).toStream

    lazy val matches = (for(
        article <- recentPublicationsOnMathSciNet; 
        others = net.tqft.webofscience.Article.fromMathSciNet(article, useGoogleScholar = true); 
        other <- others) yield {
      article -> other
    }).toMap
    lazy val onlyOnMathSciNet = {
      recentPublicationsOnMathSciNet.filterNot(matches.keySet.contains)
    }

      p("<h3>Articles found only on MathSciNet:</h3>")
      p("<ul>")
      for (a <- onlyOnMathSciNet) {
        p("<li>" + a.fullCitation + "</li>")
      }
      p("</ul>")
      p("<h3>Matching articles found:</h3>")
      p("<dl>")
      for ((a2, a1) <- matches) {
        p("<dt>" + a2.fullCitation_html + " - <a href=\"" + a1.url + "\">WOS:" + a1.accessionNumber + "</a></dt>")
        p("<dd>")

        val citations1 = a2.citations.toSeq
        val candidateMatches = a1.citations.map(c => c -> c.bestCitationMathSciNetMatch)
        val goodMatches = candidateMatches.filter(m => m._2.nonEmpty && citations1.contains(m._2.get)).map(p => (p._1, p._2.get))
        val failedMatches = candidateMatches.filter(m => m._2.isEmpty || !citations1.contains(m._2.get)).map(_._1)
        val unmatched = citations1.filterNot(r => candidateMatches.exists(_._2 == Some(r)))
        unmatchedCount = unmatchedCount + unmatched.size

        p("<table>")
        p("<tr><th>" + candidateMatches.size + " citations on Web Of Science</th><th>" + citations1.size + " citations on MathSciNet</th></tr>")
        for ((s, c) <- goodMatches) {
          p("<tr>")
          p("<td>")
          p(s.fullCitation_html)
          p("</td>")
          p("<td>")
          p(c.fullCitation_html)
          p("</td>")
          p("</tr>")
        }
        for (s <- failedMatches) {
          p("<tr>")
          p("<td>")
          p(s.fullCitation_html)
          p("</td>")
          p("<td>")
          p("</td>")
          p("</tr>")
        }
        for (c <- unmatched) {
          p("<tr>")
          p("<td>")
          p("</td>")
          p("<td>")
          p(c.fullCitation_html)
          p("</td>")
          p("</tr>")
          reportMissing(author.id, a2.fullCitation, c.fullCitation)
        }
        p("</table>")
      }
      p("</dd>")
      p("</dl>")
    

    p("</div>")
  }

  p("Found a total of " + unmatchedCount + " citations recorded in MathSciNet which Web of Science doesn't seem to know about.")

  p("</body></html>")
  FirefoxSlurp.quit

}