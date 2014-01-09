package net.tqft.util

object Angle {
	def apply(x: Map[String, Int], y: Map[String, Int]) = {
	  dot(x, y).toDouble / scala.math.sqrt((dot(x, x) * dot(y, y)).toDouble)
	}
		
	private def dot(x: Map[String, Int], y: Map[String, Int]) = {
	  var n = BigInt(0)
	  for((s, a) <- x; b <- y.get(s)) {
	    n = n + a * b
	  }
	  n
	}
}