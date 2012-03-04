package net.tqft.arxiv

trait Article {
  def identifier: String
  
  def versions: List[Version]
  def currentVersion: Version
  
  def trackbacks: List[Trackback]
  
  trait Version {
    def title: String
    def authors: List[Author]
    def `abstract`: String
    def journalReference: Option[String]
    def submittedOn: String // TODO this should be a date
    def primaryMSC: MSC
    def secondaryMSCs: List[MSC]
    def DOI: Option[DOI]
    
    def number: Int
  }
}