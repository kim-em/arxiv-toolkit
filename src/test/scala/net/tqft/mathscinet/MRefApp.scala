package net.tqft.mathscinet

object MRefApp extends App {
	println(MRef.lookup("""Fixing the functoriality of Khovanov homology
David Clark, Scott Morrison, Kevin Walker
Geom. Topol. 13 (2009), no. 3, 1499--1582.""").head.DOI.get == "10.2140/gt.2009.13.1499")
}