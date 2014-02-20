organization := "com.netaporter"

version := "0.0.3"

scalaVersion := "2.10.3"

name := "s3-spray"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

val akka = "2.2.3"
val spray = "1.2.0"

libraryDependencies ++=
  "com.typesafe.akka" %% "akka-actor" % akka % "provided,test,it" ::
  "io.spray" % "spray-client" % spray % "provided,test,it" ::
  "com.typesafe.akka" %% "akka-testkit" % akka % "provided,test,it" ::
  "org.scalatest" %% "scalatest" % "2.0" % "provided,test,it" ::
  Nil

scalariformSettings

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := (
  <url>https://github.com/net-a-porter/s3-spray</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:net-a-porter/s3-spray.git</url>
      <connection>scm:git@github.com:net-a-porter/s3-spray.git</connection>
    </scm>
    <developers>
      <developer>
        <id>theon</id>
        <name>Ian Forsey</name>
        <url>http://theon.github.io</url>
      </developer>
    </developers>)