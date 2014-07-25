package com.wbillingsley.handy.playoauth

import com.wbillingsley.handy.Ref
import play.api.mvc.{Action, EssentialAction, Request, Results, AnyContent}
import scala.util.{Try, Success, Failure}
import scala.concurrent.Promise
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Iteratee
import play.api.Play
import play.api.Play.current

object PlayAuth {
    
  /**
   * What the module should do on completion of an OAuth sign in.

   * By default, this is wired up (asynchronously) to <code>onAuth</code>. 
   * (The version that passes a <code>Try</code> rather than a <code>Ref</code>)
   */
  var onAuthR: Ref[OAuthDetails] => EssentialAction = { rur =>
    import play.api.libs.concurrent.Execution.Implicits._
    
    import scala.concurrent.promise
    val p = Promise[EssentialAction]()
    
    rur.onComplete(
      onSuccess = { ur => p.success(onAuth(Success(ur))) },
      onNone = { p.success(onAuth(Failure(AuthFailed.DECLINED))) },
      onFail = { f => p.success(onAuth(Failure(f))) }
    )
    
    EssentialAction { request => Iteratee.flatten(p.future.map(_(request))) }
  }  
  
  /**
   * What the module should do on completion of an OAuth sign in.
   * 
   * You should define this as you would with any of your controller actions. eg
   * <pre>
   * def onOAuth(tur:Try[UserRecord]) = Action { request => ... }
   * 
   * PlayAuth.onAuth = onOAuth
   * </pre>
   */
  var onAuth: Try[OAuthDetails] => EssentialAction = { toa =>
    val res = for (oa <- toa) yield Results.Ok(oa.toJson)
    res match {
      case Success(r) => Action { r }
      case Failure(f) => Action { Results.Forbidden(f.getMessage()) }
    }
  }
  
  var allowGet = Play.configuration.getBoolean("auth.oauth.allowGet").getOrElse(false)
  
  val allControllers = Seq(
    controllers.GitHubController,
    controllers.TwitterController
  )
  
  def enabledServices = allControllers.map(_.service).filter(_.available)

}

trait Service {
  val name:String
  def available:Boolean
}

trait OAuthController {
  
  def requestAuth: EssentialAction
  
  def getAuth = {
    if (PlayAuth.allowGet) requestAuth else {
      Action {
        Results.Forbidden("Initiating OAuth via GET is not enabled on this server.")
      }
    }
  }
  
  def service: Service
  
}
