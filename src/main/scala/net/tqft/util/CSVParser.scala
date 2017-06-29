package net.tqft.util

import scala.util.parsing.combinator.RegexParsers
import scala.io.Source

object CSVParser extends RegexParsers {
  def apply(f: java.io.File): Iterator[List[String]] = apply(scala.io.Source.fromFile(f))
  def apply(s: Source): Iterator[List[String]] = apply(s.getLines())
  def apply(lines: TraversableOnce[String]): Iterator[List[String]] = lines.map(apply(_)).toIterator
  def apply(s: String): List[String] = parseAll(fromCsv, s) match {
    case Success(result, _) => result
    case failure: NoSuccess => { throw new Exception("Parse Failed") }
  }

  def fromCsv: Parser[List[String]] = rep1(mainToken) ^^ { case x => x }
  def mainToken = (doubleQuotedTerm | singleQuotedTerm | unquotedTerm) <~ ",? *".r ^^ { case a => a }
  def doubleQuotedTerm: Parser[String] = "\"" ~> "[^\"]+".r <~ "\"" ^^ { case a => ("" /: a)(_ + _) }
  def singleQuotedTerm = "'" ~> "[^']+".r <~ "'" ^^ { case a => ("" /: a)(_ + _) }
  def unquotedTerm = "[^,]+".r ^^ { case a => ("" /: a)(_ + _) }

  override def skipWhitespace = false
}