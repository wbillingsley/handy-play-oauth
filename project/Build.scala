import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {
  
    val orgName         = "com.wbillingsley"
    val appName         = "handy-play-oauth"
    val appVersion      = "0.2-SNAPSHOT"
     
    val appDependencies = Seq(
      "com.wbillingsley" %% "handy" % "0.5-SNAPSHOT"	
    )

  lazy val aaaMain = play.Project(appName, appVersion, appDependencies).settings(

    organization := orgName,
      
    templatesImport += "com.wbillingsley.handy._",

    resolvers ++= Seq(
        "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
    )    
        // Add your own project settings here      
  ).dependsOn(
  )

}
