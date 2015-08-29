organization := "pirc"
name := "kpi"

crossScalaVersions := Seq("2.11.7", "2.10.5")
scalaVersion := crossScalaVersions.value.head
scalacOptions := Seq("-deprecation", "-unchecked", "-feature")
        
libraryDependencies ++= Seq (
    "com.typesafe.akka" %% "akka-kernel" % "2.3.12"
  , "com.typesafe.akka" %% "akka-remote" % "2.3.12"
  , "com.fasterxml.jackson.core" % "jackson-core" % "2.2.2"
  , "com.fasterxml.jackson.core" % "jackson-databind" % "2.2.2"
  , "javax.inject" % "javax.inject" % "1"
  , "com.google.inject" % "guice" % "3.0"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

enablePlugins(GitVersioning)
git.useGitDescribe := true // figure out version from last tag

// required for release to bintray
licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
