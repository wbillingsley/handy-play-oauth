package com.wbillingsley.handy.playoauth.controllers
import play.api.mvc.{Controller, Action}
import play.api.libs.ws.WS
import play.api.Play
import play.api.Play.current
import com.wbillingsley.handy.{Ref, RefFuture, Refused}
import com.wbillingsley.handy.playoauth._
import com.wbillingsley.handy.Ref._
import play.Logger
import play.api.mvc.EssentialAction

/**
 * Implements log in with the GitHub API
 */
object GitHubController extends Controller {

  val clientKey = Play.configuration.getString("auth.github.ckey")
  val secret = Play.configuration.getString("auth.github.csecret")
  
  case object GitHub extends Service {
    val name = "Github"
    def available = clientKey.isDefined && secret.isDefined 
  }
  
  /**
   * Beginning of the Sign in with GitHub flow, using OAuth2.
   * A random state is set in the session, and then the user is redirected to the GitHub
   * sign-in endpoint. 
   */
  def requestAuth = Action { implicit request =>    
    val randomString = java.util.UUID.randomUUID().toString()
    val returnUrl = ""
    
    if (GitHub.available) {
      Redirect(
        "https://github.com/login/oauth/authorize", 
        Map(
          "state" -> Seq(randomString),
          "client_id" -> clientKey.toSeq  
        ), 
        303
      ).withSession(request.session + ("oauth_state" -> randomString))
    } else {
      InternalServerError("This server's client key and secret for GitHub have not been set")
    }
  } 
  
  def callback = EssentialAction { implicit request =>    
    
    import play.api.libs.concurrent.Execution.Implicits._

    val stateFromSession = request.session.get("oauth_state")
    val stateFromRequest = request.getQueryString("state")

    /*
     * TODO: We've had a few errors where we were getting a mismatch between the OAuth state in the
     * session and in the callback from GitHub. For the moment, let's turn off the check and log
     * whenever there is a mismatch to see if we can uncover why. 
     */
    if (stateFromSession.isEmpty) { 
      Logger.warn("GitHub OAuth - state from session was empty")
    } 
    if (stateFromRequest.isEmpty) { 
      Logger.warn("GitHub OAuth - state from request was empty")
    } 
    if (stateFromSession != stateFromRequest) {
      Logger.warn(s"GitHub OAuth - state from request was $stateFromRequest but state from session was $stateFromSession")
    }          

    /**
     * Calls GitHub to swap a code for an auth_token
     */
    def authTokenFromCode(code:String):Ref[String] = {
      val ws = WS.url("https://github.com/login/oauth/access_token").
        withHeaders("Accept" -> "application/json").
        post(Map(
          "code" -> Seq(code),
          "client_id" -> clientKey.toSeq,
          "client_secret" -> secret.toSeq
      ))
      val authToken = for (
        resp <- new RefFuture(ws);
        tok <- { println(resp.json); (resp.json \ "access_token").asOpt[String] }
      ) yield tok
      authToken
    }
    
    /**
     * Given an authentication token, goes and looks up that user's details on GitHub.
     * These are filled into an "Interstitial Memory" -- details to remember during the display
     * of the confirmation page.
     */
    def userFromAuth(authToken:String) = {
      val ws = WS.url("https://api.github.com/user").
                 withHeaders(
                   "Accept" -> "application/json",
                   "Authorization" -> ("token " + authToken)
                 ).get()
      
      for (
        resp <- new RefFuture(ws);
        json = resp.json;
        id <- (resp.json \ "id").asOpt[Int].map(_.toString)
      ) yield {
        OAuthDetails(
          userRecord = UserRecord(
            service = "github",
            id = id,
            name = (json \ "name").asOpt[String],
            nickname = (json \ "login").asOpt[String],
            username = (json \ "login").asOpt[String],
            avatar = (json \ "avatar_url").asOpt[String]
          ),
          raw = Some(json)
        )
      }
    }    
    
    val refMem = for (
      code <- Ref(request.getQueryString("code")) orIfNone Refused("GitHub provided no code");
      authToken <- authTokenFromCode(code) orIfNone Refused("GitHub did not provide an authorization token");
      mem <- userFromAuth(authToken) orIfNone Refused("GitHub did not provide any user data for that login")
    ) yield mem
    
    PlayAuth.onAuthR(refMem)(request)
  }
  
}