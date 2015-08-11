organization := "pirc"
name := "kpi"

crossScalaVersions := Seq("2.11.7", "2.10.5")
scalaVersion := crossScalaVersions.value.head
scalacOptions := Seq("-deprecation", "-unchecked", "-feature")

libraryDependencies ++= Seq (
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

enablePlugins(GitVersioning)
git.useGitDescribe := true // figure out version from last tag

// required for release to bintray
licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
