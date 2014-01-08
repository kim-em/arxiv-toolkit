package net.tqft.mathscinet

object SQLImportApp extends App {

  import scala.slick.driver.MySQLDriver.simple._

//  scala.slick.model.codegen.SourceCodeGenerator.main(
//  Array("scala.slick.driver.MySQLDriver", "com.mysql.jdbc.Driver", url, outputFolder, pkg)
//)
  
  Database.forURL("jdbc:mysql://mysql.tqft.net/mathematicsliteratureproject", driver = "com.mysql.jdbc.Driver") withSession {
    implicit session =>
    // <- write queries here
  }

}