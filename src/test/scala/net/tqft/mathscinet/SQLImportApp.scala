package net.tqft.mathscinet

object SQLImportApp extends App {

  import scala.slick.driver.MySQLDriver.simple._

  Database.forURL("jdbc:mysql://mysql.tqft.net/mathematicsliteratureproject", driver = "com.mysql.jdbc.Driver") withSession {
    implicit session =>
    // <- write queries here
  }

}