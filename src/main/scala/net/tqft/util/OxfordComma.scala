package net.tqft.util

object OxfordComma {
  implicit class OxfordComma(phrases: Seq[String]) {
    def oxfordComma = {
      phrases match {
        case Seq() => ""
        case Seq(one) => one
        case Seq(one, two) => one + " and " + two
        case most :+ last => most.mkString(", ") + ", and " + last
      }
    }
  }
}