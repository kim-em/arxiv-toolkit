package net.tqft.util

object pandocApp extends App {
	println(pandoc.latexToText("""On some series containing {$\psi(x)-\psi(y)$} and {$(\psi(x)-\psi(y))^2$} for certain values of {$x$} and {$y$}"""))
}