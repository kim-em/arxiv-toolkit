package net.tqft.scopus

import net.tqft.util.Slurp
import net.tqft.util.FirefoxSlurp

object Scopus {
  lazy val preload = {
    //  use FirefoxSlurp directly, to avoid the cache on this first hit.
    (new FirefoxSlurp {})("http://www.scopus.com/")
    None
  }
}

case class Author(id: Long, name: String) {
  def URL = "http://www.scopus.com/authid/detail.url?authorId=" + id.toString

  def publicationsURL = "http://www.scopus.com/search/submit/author.url?authorId=" + id.toString

  def publications: Iterator[Article] = {
//    <a onclick="javascript:submitRecord('2-s2.0-70349820748','9','5');" title="Show document details" href="http://www.scopus.com/record/display.url?eid=2-s2.0-70349820748&amp;origin=resultslist&amp;sort=plf-f&amp;src=s&amp;sid=6995D3618DB9D6D6ACB543BC41D0E039.FZg2ODcJC9ArCe8WOZPvA%3a20&amp;sot=aut&amp;sdt=a&amp;sl=35&amp;s=AU-ID%28%22Morrison%2c+Scott%22+7201672329%29&amp;relpos=9&amp;relpos=9&amp;citeCnt=5&amp;searchTerm=AU-ID%28%5C%26quot%3BMorrison%2C+Scott%5C%26quot%3B+7201672329%29">Skein theory for the D2 n planar algebras</a>

    val r = """<a onclick="javascript:submitRecord\('(.*)','[0-9]*','[0-9]*'\);" title="Show document details" href=".*">(.*)</a>""".r
    for (line <- Slurp(publicationsURL); if line.contains("Show document details"); m <- r.findAllMatchIn(line)) yield {
      Article(m.group(1), m.group(2))
    }
  }
}