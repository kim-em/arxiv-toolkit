package net.tqft.mathscinet

trait Author {
	def name: String
	def lastName = name.takeWhile(c => c != ',' && c != ' ')
}

object Author {
  def apply(name: String) = {
    val name_ = name
    new Author {
      override val name = name_
    }
  }
}