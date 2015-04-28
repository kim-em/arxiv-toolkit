package net.tqft.arxiv2

import java.sql.Date
import net.tqft.util.HTMLEncode

case class Article(
  identifier: String,
  created: Date,
  updated: Option[Date],
  authorsRaw: String,
  title: String,
  categoriesRaw: String,
  comments: Option[String],
  proxy: Option[String],
  reportno: Option[String],
  mscclassRaw: Option[String],
  acmclassRaw: Option[String],
  journalref: Option[String],
  doi: Option[String],
  license: Option[String],
  `abstract`: String) {
  def sqlRow = (identifier, created, updated, authorsRaw, title, categoriesRaw, comments, proxy, reportno, mscclassRaw, acmclassRaw, journalref, doi, license, `abstract`)
  lazy val categories = categoriesRaw.split(" ").toList
  lazy val authors = {
    val regex = "<keyname>(.*)</keyname>(?:<forenames>(.*)</forenames>)?(?:<suffix>(.*)</suffix>)?(?:<affiliation>(.*)</affiliation>)?".r
    HTMLEncode.decode(authorsRaw).stripPrefix("<author>").stripSuffix("</author>").split("</author><author>").toList.map({
      case regex(keyname, forenames, suffix, affiliation) => (keyname,Option(forenames).getOrElse(""), Option(suffix), Option(affiliation))
    })
  }
}