import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {
  
    val orgName         = "com.wbillingsley"
    val appName         = "handy-play-oauth"
    val appVersion      = "0.1"
     
    val appDependencies = Seq(
      "com.wbillingsley" %% "handy" % "0.4",
      "com.wbillingsley" %% "handy-play" % "0.4",
      "com.wbillingsley" %% "salt-encrypt" % "0.1"      
    )

  lazy val aaaMain = play.Project(appName, appVersion, appDependencies).settings(

    organization := orgName,
      
    templatesImport += "com.wbillingsley.handy._",

    resolvers ++= Seq(
        "handy releases" at "https://bitbucket.org/wbillingsley/mavenrepo/raw/master/releases/",
        "handy snapshots" at "https://bitbucket.org/wbillingsley/mavenrepo/raw/master/snapshots/",
        "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
    )    
        // Add your own project settings here      
  ).dependsOn(
  )

}
