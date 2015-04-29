package net.tqft.webofscience

import net.tqft.util.FirefoxSlurp

object WebOfScience {
  private var latch = true
  lazy val preload = {
    //  use FirefoxSlurp directly, to avoid the cache on this first hit.
    if (latch) {
      latch = false
      (new FirefoxSlurp {})("http://apps.webofknowledge.com/")
    }
    None
  }
}
