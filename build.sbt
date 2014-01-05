name := "arxiv-toolkit"

organization := "net.tqft"

version := "0.1.1-SNAPSHOT"

scalaVersion := "2.10.3"

resolvers ++= Seq(
	"Java.net Maven2 Repository" at "http://download.java.net/maven/2/",
	"tqft.net Maven repository" at "http://tqft.net/releases",
	"Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
	"Sonatype Nexus Releases" at "https://oss.sonatype.org/content/repositories/releases",
	"Scala Snapshots" at "http://scala-tools.org/repo-snapshots/"
)

// Project dependencies
libraryDependencies ++= Seq(
	"net.tqft" %% "toolkit-base" % "0.1.16-SNAPSHOT",
	"net.tqft" %% "toolkit-collections" % "0.1.16-SNAPSHOT",
	"net.tqft" %% "toolkit-amazon" % "0.1.16-SNAPSHOT",
	"net.tqft" %% "toolkit-wiki" % "0.1.16-SNAPSHOT",
	"com.ibm.icu" % "icu4j" % "51.1",
	"rome" % "rome" % "1.0",
	"org.apache.httpcomponents" % "httpclient" % "4.1.1", // jets3t can't cope with 4.2, but 4.3.2 should be okay again
	"commons-io" % "commons-io" % "2.4",
	"com.google.code.findbugs" % "jsr305" % "2.0.1",
	"net.sourceforge.htmlunit" % "htmlunit" % "2.12",
	"org.seleniumhq.selenium" % "selenium-support" % "2.39.0",
	"org.seleniumhq.selenium" % "selenium-htmlunit-driver" % "2.39.0",
	"org.seleniumhq.selenium" % "selenium-firefox-driver" % "2.39.0",
	"org.seleniumhq.selenium" % "selenium-chrome-driver" % "2.39.0",
	"be.roam.hue" % "hue" % "1.1",
	"eu.medsea.mimeutil" % "mime-util" % "2.1.3",
	"mysql" % "mysql-connector-java" % "5.1.24",
	"com.typesafe.slick" %% "slick" % "2.0.0-RC1"
)

dependencyOverrides += "org.apache.httpcomponents" % "httpclient" % "4.1.1" // jets3t can't cope with 4.2

// lift-json
libraryDependencies ++= {
  val liftVersion = "2.6-M2" // Put the current/latest lift version here
  Seq(
    "net.liftweb" %% "lift-util" % liftVersion,
    "net.liftweb" %% "lift-json" % liftVersion,
    "net.liftweb" %% "lift-json-ext" % liftVersion
    )
}

// Test dependencies
libraryDependencies ++= Seq(
	"junit" % "junit" % "4.8" % "test",
	"org.scalatest" %% "scalatest" % "1.9.1" % "compile,test"
)

// Sometimes it's useful to see debugging out from the typer (e.g. to resolve slow compiles)
// scalacOptions += "-Ytyper-debug"

publishTo := Some(Resolver.sftp("tqft.net", "tqft.net", "tqft.net/releases") as ("scottmorrison", new java.io.File("/Users/scott/.ssh/id_rsa")))

net.virtualvoid.sbt.graph.Plugin.graphSettings
