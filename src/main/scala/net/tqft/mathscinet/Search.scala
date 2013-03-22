package net.tqft.mathscinet

import net.tqft.util.Slurp

object Search {
  // TODO remove; this is in the toolkit
  implicit def splittableIterator[A](x: Iterator[A]) = new SplittableIterator(x)
  class SplittableIterator[A](x: Iterator[A]) {
    def splitAfter(p: A => Boolean): Iterator[List[A]] = {
      new Iterator[List[A]] {
        def hasNext = x.hasNext
        def next = {
          import net.tqft.toolkit.collections.TakeToFirst._
          x.takeToFirst(p)
        }
      }
    }

    def splitOn(p: A => Boolean) = splitAfter(p).map(s => if (p(s.last)) s.init else s)

  }

  
  def query(q: String): Iterator[Article] = {
    def queryString(k: Int) = "http://www.ams.org/mathscinet/search/publications.html?" + q + "&r=" + (1 + 100 * k).toString + "&extend=1&fmt=bibtex"
    def queries = Iterator.from(0).map(queryString).map(Slurp.attempt).takeWhile(_.isLeft).map(_.left.get).flatten.takeWhile(_ != """<span class="disabled">Next</span>""")
    def bibtexChunks = queries.splitOn(line => line.trim == "<pre>" || line.trim == "</pre>").grouped(2).filter(_.size == 2).map(_(1).mkString("\n"))
    bibtexChunks.flatMap(Article.fromBibtex)
  }
}