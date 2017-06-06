package net.tqft.zentralblatt

import net.tqft.util.Slurp

object CitationMatching {
  def apply(query: String): String = {
    Slurp("https://zbmath.org/citationmatching/mathoverflow?q=" + query).mkString("\n")
  }
}