package net.tqft.scopus

import net.tqft.util.FirefoxSlurp

object AuthorApp extends App {
	for(p <- Author(7201672329L).publications) {
	  println(p)
	}
	
	FirefoxSlurp.quit
}