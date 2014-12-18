package net.tqft.mlp

import net.tqft.mathscinet.Search
import net.tqft.journals.ISSNs
import net.tqft.toolkit.wiki.WikiMap
import net.tqft.mathscinet.Article
import net.tqft.util.pandoc
import net.tqft.eigenfactor.Eigenfactor
import net.tqft.util.FirefoxSlurp

object TitleBot extends App {

  FirefoxSlurp.disable

  lazy val titlebot = {
    val b = WikiMap("http://tqft.net/mlp/index.php")
    b.login("titlebot", "zytopex")
    b.enableSQLReads("jdbc:mysql://mysql.tqft.net/mathematicsliteratureproject?user=readonly1&password=readonly", "mlp_")
    b.setThrottle(45000)
    b
  }

  //  Article.enableBibtexSaving

  for (a <- extendedCoverage ++ topJournals(100)) {
    println("posting title for " + a.identifierString + ": " + a.title + " // " + a.wikiTitle)
    titlebot("Data:" + a.identifierString + "/Title") = a.wikiTitle
  }

  println("Done entering titles!")

  net.tqft.toolkit.wiki.FirefoxDriver.quit
}