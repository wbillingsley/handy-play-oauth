import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {
  
    val orgName         = "com.wbillingsley"
    val appName         = "handy-play-oauth"
    val appVersion      = "0.2-SNAPSHOT"
     
    val appDependencies = Seq(
      "com.wbillingsley" %% "handy" % "0.5.0-RC1"
    )

  lazy val aaaMain = play.Project(appName, appVersion, appDependencies).settings(

    organization := orgName,
      
    templatesImport += "com.wbillingsley.handy._",

    resolvers ++= Seq(
        "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",

        "bintrayW" at "http://dl.bintray.com/wbillingsley/maven"

    )    
        // Add your own project settings here      
  ).dependsOn(
  )

}
