package net.tqft

package object mlp {
  import net.tqft.journals.ISSNs
  import net.tqft.mathscinet.Search

  def selectedJournals = Iterator(
    ISSNs.`Advances in Mathematics`,
    ISSNs.`Discrete Mathematics`,
    ISSNs.`Annals of Mathematics`,
    ISSNs.`Algebraic & Geometric Topology`,
    ISSNs.`Geometric and Functional Analysis`,
    ISSNs.`Journal of Functional Analysis`,
    ISSNs.`Journal of Number Theory`,
    ISSNs.`Journal of Combinatorial Theory A`,
    ISSNs.`Journal of Combinatorial Theory B`
    )

  val years = Seq(2010, 2013)

  def currentCoverage = for (j <- selectedJournals; y <- years; a <- Search.inJournalYear(j, y)) yield a

}