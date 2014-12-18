package net.tqft.mlp

import net.tqft.mathscinet.Search
import net.tqft.journals.ISSNs
import net.tqft.toolkit.wiki.WikiMap
import net.tqft.mathscinet.Article
import net.tqft.util.pandoc
import net.tqft.eigenfactor.Eigenfactor
import net.tqft.util.FirefoxSlurp

object DOIBot extends App {

  FirefoxSlurp.disable

  lazy val doibot = {
    val b = WikiMap("http://tqft.net/mlp/index.php")
    b.login("doibot", "zytopex")
    b.enableSQLReads("jdbc:mysql://mysql.tqft.net/mathematicsliteratureproject?user=readonly1&password=readonly", "mlp_")
    b.setThrottle(10000)
    b
  }

  for (a <- extendedCoverage ++ topJournals(100)) {
    for (link <- a.DOI.map("http://dx.doi.org/" + _).orElse(a.URL)) {
      println("posting link for " + a.identifierString)
      doibot("Data:" + a.identifierString + "/PublishedURL") = link

    }
  }

  println("Done entering DOIs!")

  net.tqft.toolkit.wiki.FirefoxDriver.quit
}