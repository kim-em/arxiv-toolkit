package net.tqft.arxiv
import scala.io.Source
import net.tqft.util.Slurp
import java.util.Date
import java.io.InputStream
import net.tqft.citation.Citation
import net.tqft.util.Html

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
    val content = Slurp("http://arxiv.org/tb/" + identifier).mkString("\n")
    ???
  }

  trait Version {
    private lazy val abs = Html.usingSlurp(Slurp).jQuery("http://arxiv.org/abs/" + identifier + "v" + number)
    
    def title: String = ???
    def authors: List[Author] = ???
    def `abstract`: String = ???
    def journalReference: Option[String] = {
      //<td class="tablecell label">
      //Journal&nbsp;reference:
      //</td>
      //<td class="tablecell jref">Proceedings of the National Academy of Sciences, May 17, 2011,
      //  vol. 108 no. 20, pp. 8139-8145</td>
      //</tr>
      
      val element = abs.get("td.jref")
      
      if(element.size == 0) {
        None
      } else if(element.size == 1){
        Some(element.first.trimmedText)
      } else {
        ???
      }
    }
    def submittedOn: Date = ???
    def primaryMSC: MSC = ???
    def secondaryMSCs: List[MSC] = ???
    def DOI: Option[String] = ???

    def number: Int

    def source: Map[String, InputStream] = ???
    def pdf: InputStream = ???

    def references: Iterator[Citation] = {
      for (file <- source.keysIterator; if file.endsWith(".tex") || file.endsWith(".bbl")) yield {
        ???
      }
    }
  }
}

object Article {
  import com.sun.syndication.feed.synd._
  import com.sun.syndication.io.SyndFeedInput

  def fromAtomEntry(entry: SyndEntry, currentVersion_? : Boolean): Article = {
    val urlRegex = "http://arxiv.org/abs/(.*)".r
    val urlRegex(returnedIdentifier) = entry.getUri

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
        override val submittedOn = entry.getUpdatedDate()
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