package net.tqft.arxiv2

case class Author(id: Int, keyname: String, forenames: String, suffix: Option[String], affiliation: Option[String]) {
  def sqlRow = (id, keyname, forenames, suffix, affiliation)
  def fullName = (forenames + " " + keyname + suffix.map(", " + _).getOrElse("")).trim
}