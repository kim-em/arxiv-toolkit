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

object CompareScopusAndMathSciNetApp extends App {

  val outputFile = new File(System.getProperty("user.home") + "/projects/arxiv-toolkit/compare.html")
  outputFile.delete
  val out = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8")
  def p(s: String) = {
    println(s)
    out.write(s + "\n")
    out.flush
  }
  val missing = new File(System.getProperty("user.home") + "/projects/arxiv-toolkit/missing-from-scopus")
  missing.delete
  val missing_out = new OutputStreamWriter(new FileOutputStream(missing), "UTF-8")
  def reportMissing(scopusAuthorId: Long, target: String, source: String) = {
    missing_out.write(scopusAuthorId + "\n")
    missing_out.write(target + "\n")
    missing_out.write(source + "\n")
    missing_out.write("\n")
    missing_out.flush
  }

  val notFound = new File(System.getProperty("user.home") + "/projects/arxiv-toolkit/not-found-on-scopus")
  notFound.delete
  val notFound_out = new OutputStreamWriter(new FileOutputStream(notFound), "UTF-8")
  def reportNotFound(citation: String) = {
    notFound_out.write(citation + "\n")
    missing_out.write("\n")
    missing_out.flush
  }
  
  val mathematicians = (for (
    line <- io.Source.fromFile(new File(System.getProperty("user.home") + "/projects/arxiv-toolkit/mathematicians.txt"))(Codec.UTF8).getLines;
    if line.nonEmpty && !line.startsWith("#");
    fields = CSVParser(line)
  ) yield fields).toList

  val authors = for (
    Int(mathscinetAuthorId) :: Long(scopusAuthorId) :: name :: "ANU" :: level :: _ <- mathematicians//;
//    if mathscinetAuthorId == 40355 //Peter
//    if mathscinetAuthorId == 32465
  ) yield {
    (Author(mathscinetAuthorId, name), net.tqft.scopus.Author(scopusAuthorId, name))
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
  var citationsOnMathSciNet = 0
  var citationsOnScopus = 0

  for ((ma, sa) <- authors) {
    p("""<h2><a onclick="$('#publications-""" + ma.id + s"""').toggle('fast')">Publications for <i>${ma.name}</i> since $firstYear</a></h2>""")
    p(s"<div id='publications-${ma.id}' style='display: none'>")

    lazy val recentPublicationsOnScopus = sa.publications.filter(a => a.yearOption.nonEmpty && a.yearOption.get >= firstYear)
    lazy val recentPublicationsOnMathSciNet = ma.articles.filter(a => a.yearOption.nonEmpty && a.yearOption.get >= firstYear).toStream

    lazy val onlyOnScopus = recentPublicationsOnScopus.filter(_.satisfactoryMatch.isEmpty)
    lazy val tentativeMatches = recentPublicationsOnScopus.map(p => (p, p.satisfactoryMatch)).collect({
      case (p, Some(CitationScore(c, _))) if c.MRNumber.nonEmpty => (p, Article(c.MRNumber.get))
    })
    lazy val matches = tentativeMatches.groupBy(_._2.identifier).filter(_._2.size == 1).map(_._2.head)
    lazy val onlyOnMathSciNet = {
      if (sa.id > 0) {
        val matchedMathSciNetIds = matches.map(_._2.identifier).toSet
        recentPublicationsOnMathSciNet.filterNot(a => matchedMathSciNetIds.contains(a.identifier))
      } else {
        recentPublicationsOnMathSciNet
      }
    }

    if (sa.id > 0) {
      p("<h3>Articles found only on Scopus:</h3>")
      p("<dl>")
      for (a <- onlyOnScopus) {
        citationsOnScopus = citationsOnScopus + a.citations.size
        p("<dt>" + a.fullCitation_html + "</dt>")
        for (m <- a.matches.headOption)
          p(s"<dd>best match (${m.score}): ${fullCitation_html(m.citation)}</dd>")
      }
      p("</dl>")
    }
    if (ma.id > 0) {
      p("<h3>Articles found only on MathSciNet:</h3>")
      p("<ul>")
      for (a <- onlyOnMathSciNet) {
        citationsOnMathSciNet = citationsOnMathSciNet + a.citations.size
        p("<li>" + a.fullCitation + "</li>")
        reportNotFound(a.fullCitation)
      }
      p("</ul>")
    }
    if (sa.id > 0) {
      p("<h3>Matching articles found:</h3>")
      p("<dl>")
      for ((a1, a2) <- matches) {
        citationsOnScopus = citationsOnScopus + a1.citations.size
                citationsOnMathSciNet = citationsOnMathSciNet + a2.citations.size

        p("<dt>" + a1.fullCitation_html + "</dt>")
        p("<dd>" + a2.fullCitation_html)

        val citations1 = a2.citations.toSeq
        val candidateMatches = a1.bestCitationMathSciNetMatches
        val goodMatches = candidateMatches.filter(m => m._2.nonEmpty && citations1.contains(m._2.get)).map(p => (p._1, p._2.get))
        val failedMatches = candidateMatches.filter(m => m._2.isEmpty || !citations1.contains(m._2.get)).map(_._1)
        val unmatched = citations1.filterNot(r => candidateMatches.exists(_._2 == Some(r)))
        unmatchedCount = unmatchedCount + unmatched.size

        p("<table>")
        p("<tr><th>" + candidateMatches.size + " citations on Scopus</th><th>" + citations1.size + " citations on MathSciNet</th></tr>")
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
          reportMissing(sa.id, a1.fullCitation, c.fullCitation)
        }
        p("</table>")
      }
      p("</dd>")
      p("</dl>")
    }

    p("</div>")
  }

  p("Found a total of " + unmatchedCount + " citations recorded in MathSciNet which Scopus doesn't seem to know about.")
  p("Scopus records a total of " + citationsOnScopus + " citations")
  p("MathSciNet records a total of " + citationsOnMathSciNet + " citations")

  p("</body></html>")
  FirefoxSlurp.quit

}