package net.tqft.mathscinet

import net.tqft.mlp.sql.SQL
import net.tqft.mlp.sql.SQLTables
import slick.jdbc.MySQLProfile.api._
import net.tqft.toolkit.Logging
import net.tqft.util.pandoc
import scala.collection.parallel.ForkJoinTaskSupport

object SQLDeleteAuxApp extends App {

  SQL {
    SQLTables.mathscinet_aux.filter(_.textCitation === "").delete
  }

}