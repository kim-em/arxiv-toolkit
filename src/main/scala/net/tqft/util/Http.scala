package net.tqft.util

import java.net.URL
import java.net.HttpURLConnection

object Http {
  def findRedirect(url: String) = {
    val connection = new URL(url).openConnection().asInstanceOf[HttpURLConnection]
    connection.setInstanceFollowRedirects(false);
    if (connection.getResponseCode == 303) {
      Some(connection.getHeaderField("Location"))
    } else {
      None
    }
  }
}