scalaVersion in ThisBuild := "2.11.1"

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature")

crossScalaVersions in ThisBuild := Seq("2.11.1")

licenses in ThisBuild := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php"))

homepage in ThisBuild := Some(url("http://wbillingsley.github.io/handy-play-oauth"))

publishMavenStyle in ThisBuild := true

publishTo in ThisBuild <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomExtra in ThisBuild := (
  <scm>
    <url>git@github.com:wbillingsley/handy-play-oauth.git</url>
    <connection>scm:git:git@github.com:wbillingsley/handy-play-oauth.git</connection>
  </scm>
    <developers>
      <developer>
        <id>wbillingsley</id>
        <name>William Billingsley</name>
        <url>http://www.wbillingsley.com</url>
      </developer>
    </developers>
  )

lazy val root = (project in file(".")).enablePlugins(PlayScala)

name := "handy-play-oauth"

organization := "com.wbillingsley"

version := "0.3.0-SNAPSHOT"

parallelExecution in Test := false

TwirlKeys.templateImports += "com.wbillingsley.handy._"

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  "bintrayW" at "http://dl.bintray.com/wbillingsley/maven"
)


libraryDependencies ++= Seq(
  "com.wbillingsley" %% "handy" % "0.6.0-SNAPSHOT",
  ws
)

