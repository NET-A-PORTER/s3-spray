import sbt._
import Keys._

object S3Build extends Build {
  lazy val root = Project("s3-spray", file("."))
    .configs( IntegrationTest )
    .settings( Defaults.itSettings: _* )
}