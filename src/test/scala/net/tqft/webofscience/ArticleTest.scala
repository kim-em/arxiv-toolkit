package net.tqft.webofscience

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import net.tqft.toolkit.Throttle

@RunWith(classOf[JUnitRunner])
class ArticleTest extends FlatSpec with ShouldMatchers {
  //  "fromMathSciNet" should "use a database, falling back to fromDOI when needed" in {
  //     Article.fromMathSciNet(net.tqft.mathscinet.Article("MR2979509")) should equal(Some(Article("000309470900002")))
  //  }

  "citations" should "find the right number of citations" in {
    val citations = Article("000309470900002").citations
    for (c <- citations) println(c)
    citations.size should equal(15)
  }
  "citations" should "cope with multipage lists" in {
    Article("A1983QN41500001").citations.size should equal(650)
  }

  //  "fromDOI" should "use Google Scholar to find the Web of Science accession number" in {
  //    Article.fromDOI("10.1007/BF01389127") should equal(Some(Article("A1983QN41500001")))
  //  }
}
