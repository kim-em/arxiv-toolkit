package net.tqft.util

object URLEncode {
	def apply(s: String) = net.liftweb.util.Helpers.urlEncode(s)
}