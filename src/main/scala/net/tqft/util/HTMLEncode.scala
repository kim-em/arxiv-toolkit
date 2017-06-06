package net.tqft.util

import org.apache.commons.lang3.StringEscapeUtils

object HTMLEncode {
	def decode(s: String): String = {
	  StringEscapeUtils.unescapeHtml4(s)
	}
	def encode(s: String): String = {
	  StringEscapeUtils.escapeHtml4(s)
	}
}