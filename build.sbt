name := "arxiv-toolkit"

organization := "net.tqft"

version := "0.1.0"

scalaVersion := "2.9.1"

resolvers ++= Seq(
	"Java.net Maven2 Repository" at "http://download.java.net/maven/2/",
	"tqft.net Maven repository" at "http://tqft.net/releases",
	"Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
	"Sonatype Nexus Releases" at "https://oss.sonatype.org/content/repositories/releases",
	"Scala Snapshots" at "http://scala-tools.org/repo-snapshots/"
)

// Project dependencies
libraryDependencies ++= Seq(
	"net.tqft.toolkit" %% "toolkit" % "0.1.3",
	"rome" % "rome" % "1.0" withJavadoc(),
	"org.bowlerframework" % "core_2.9.1" % "0.5.1",
	"org.apache.httpcomponents" % "httpclient" % "4.1.1"
)

// lift stuff
libraryDependencies ++= {
  val liftVersion = "2.4-M4" // Put the current/latest lift version here
  Seq(
    "net.liftweb" % "lift-json_2.9.1" % liftVersion % "compile->default",
    "net.liftweb" % "lift-json-ext_2.9.1" % liftVersion % "compile->default",
    "net.liftweb" % "lift-util_2.9.1" % liftVersion % "compile->default"
    )
}

// Test dependencies
libraryDependencies ++= Seq(
	"junit" % "junit" % "4.8" % "test",
	"org.scalatest" % "scalatest_2.9.0" % "1.6.1" % "compile,test",
	"org.mortbay.jetty" % "jetty" % "6.1.22" % "test"
)

// Dependencies for jetty
libraryDependencies ++= Seq(
	"org.mortbay.jetty" % "jetty" % "6.1.22" % "container",
	"javax.servlet" % "servlet-api" % "2.5" % "provided" 
)

// Sometimes it's useful to see debugging out from the typer (e.g. to resolve slow compiles)
// scalacOptions += "-Ytyper-debug"

// Setting up the webapp
seq(webSettings :_*)

port in container.Configuration := 8083

// We use method dependent types, which for now are an experimental feature. (But are in trunk, probably landing in 2.10.)
scalacOptions ++= Seq("-Xexperimental")

// The Scala X-Ray plugin produces nicely formatted source code.
// addCompilerPlugin("org.scala-tools.sxr" % "sxr_2.9.0" % "0.2.7")

// scalacOptions <+= scalaSource in Compile map { "-P:sxr:base-directory:" + _.getAbsolutePath }

publishTo := Some(Resolver.sftp("tqft.net Maven repository", "tqft.net", "tqft.net/releases") as ("scottmorrison", new java.io.File("/Users/scott/.ssh/id_rsa")))
