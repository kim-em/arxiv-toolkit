package net.tqft.mlp

import net.tqft.mathscinet.Search
import net.tqft.journals.ISSNs
import net.tqft.toolkit.wiki.WikiMap
import net.tqft.mathscinet.Article
import net.tqft.util.pandoc
import net.tqft.eigenfactor.Eigenfactor
import net.tqft.util.FirefoxSlurp

object AuthorBot extends App {

  
  FirefoxSlurp.disable
  
  lazy val authorbot = {
    val b = WikiMap("http://tqft.net/mlp/index.php")
    b.login("authorbot", "zytopex")
    b.enableSQLReads("jdbc:mysql://mysql.tqft.net/mathematicsliteratureproject?user=readonly1&password=readonly", "mlp_")
    b.setThrottle(5000)
    b
  }

  //  Article.enableBibtexSaving

  for (a <- extendedCoverage ++ topJournals(100)) {
    println("posting authors for " + a.identifierString)
    authorbot("Data:" + a.identifierString + "/Authors") = a.authors.map(a => pandoc.latexToText(a.name)).mkString(" and ")
  }

  println("Done entering authors!")

  net.tqft.toolkit.wiki.FirefoxDriver.quit
}