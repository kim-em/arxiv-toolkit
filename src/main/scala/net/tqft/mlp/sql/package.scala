package net.tqft.mlp

package object sql {
  type bibtexTuple = (Int, String, Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], (Option[String], Option[String]))

  val db = {
    import slick.driver.MySQLDriver.api._
    Database.forURL("jdbc:mysql://mysql.tqft.net/mathematicsliteratureproject?user=mathscinetbot&password=zytopex", driver = "com.mysql.jdbc.Driver")
  }
}