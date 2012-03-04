package net.tqft.arxiv
import scala.io.Source

trait Article {
  def identifier: String

  protected var currentVersionNumber: Option[Int] = None
  private def ensureCurrentVersionNumberKnown {
    if (currentVersionNumber.isEmpty) {
      ???
    }
  }

  protected val versionsCache: scala.collection.mutable.Map[Int, Version] = scala.collection.mutable.Map()
  private def populateVersionsCache {
    ensureCurrentVersionNumberKnown
    for (i <- 1 to currentVersionNumber.get) {
      versionsCache.getOrElseUpdate(i, new Version { val number = i })
    }
  }

  def versions: List[Version] = {
    populateVersionsCache
    for (i <- (1 to currentVersionNumber.get).toList) yield versionsCache(i)
  }
  def currentVersion: Version = versions.last

  def trackbacks: List[Trackback] = {
    val content = Source.fromURL("http://arxiv.org/tb/" + identifier).toString
    ???
  }

  trait Version {
    def title: String = ???
    def authors: List[Author] = ???
    def `abstract`: String = ???
    def journalReference: Option[String] = ???
    def submittedOn: String = ??? // TODO this should be a date
    def primaryMSC: MSC = ???
    def secondaryMSCs: List[MSC] = ???
    def DOI: Option[String] = ???

    def number: Int
  }
}

object Article {
  import com.sun.syndication.feed.synd._
  import com.sun.syndication.io.SyndFeedInput

  def fromAtomEntry(entry: SyndEntry, currentVersion_? : Boolean): Article = {
    val url = "http://arxiv.org/abs/(.*)".r
    val url(returnedIdentifier) = entry.getUri

    val versionNumber = Identifiers.extractVersionNumber(returnedIdentifier).get

    new Article {
      override val identifier = Identifiers.stripVersionNumber(returnedIdentifier)

      if (currentVersion_?) {
        currentVersionNumber = Some(versionNumber)
      }

      versionsCache += ((versionNumber, new Version {
        override val number = versionNumber
        // TODO override more stuff

        import scala.collection.JavaConverters._
        override val title = entry.getTitle()
        override val authors = entry.getAuthors.asScala.toList.asInstanceOf[List[SyndPerson]].flatMap(sp => Author.lookup(sp.getName))
        override val journalReference = None
        override val DOI = (for (
          link <- entry.getLinks.asScala.toList.asInstanceOf[List[SyndLink]];
          if (link.getTitle == "doi")
        ) yield link.getHref.stripPrefix("http://dx.doi.org/")).headOption
      }))
    }

  }

  def fromAtomEntry(entry: SyndEntry, queriedIdentifier: Option[String] = None): Article = {

    val currentVersion_? = queriedIdentifier match {
      case None => false
      case Some(qid) => Identifiers.extractVersionNumber(qid) match {
        case None => true
        case Some(v) => false
      }
    }

    fromAtomEntry(entry, currentVersion_?)
  }
}