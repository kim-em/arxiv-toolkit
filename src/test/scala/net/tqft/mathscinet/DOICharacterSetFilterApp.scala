package net.tqft.mathscinet

import net.tqft.util.BIBTEX
import net.tqft.util.FirefoxSlurp

object DOICharacterSetFilterApp extends App {
  Article.disableBibtexSaving
  FirefoxSlurp.disable

  for (
    article <- Articles.fromBibtexDirectory(System.getProperty("user.home") + "/Literature/mathscinet-data/bibtex/");
    doi <- article.DOI
  	if !doi.matches("10[.][-A-Za-z0-9._;()/]*")
  ) {
    println(article.bibtex.toBIBTEXString)
  }
}