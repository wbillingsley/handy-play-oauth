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

import play.api.libs.json._

import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Implements log in with the Google+ API, server-side flow
 */
object GooglePlusController extends Controller with OAuthController {

  val clientKey = Play.configuration.getString("auth.google.ckey")
  val secret = Play.configuration.getString("auth.google.csecret")
  
  case object GooglePlus extends Service {
    val name = "Google+"
    def available = clientKey.isDefined && secret.isDefined 
  }
  
  def service = GooglePlus
  
  /**
   * Beginning of the Sign in with GitHub flow, using OAuth2.
   * A random state is set in the session, and then the user is redirected to the GitHub
   * sign-in endpoint. 
   */
  def requestAuth = Action { implicit request =>

    val randomString = java.util.UUID.randomUUID().toString()
    
    if (GooglePlus.available) {
      Redirect(
        "https://accounts.google.com/o/oauth2/auth",
        Map(
          // TODO: remove approval_prompt when we're sure things are working. This parameter forces the user to
          // reapprove the app, and causes Google to include the refresh token in the response.
          //"approval_prompt" -> "force",

          "scope" -> Seq("https://www.googleapis.com/auth/plus.login"),
          "response_type" -> Seq("code"),
          "redirect_uri" -> Seq(routes.GooglePlusController.callback().absoluteURL()),
          "access_type" -> Seq("online"), // TODO: allow change to offline to get a refresh token (config option)
          "state" -> Seq(randomString),
          "client_id" -> clientKey.toSeq  
        ), 
        303
      ).withSession(request.session + ("oauth_state" -> randomString))
    } else {
      InternalServerError("This server's client key and secret for Google have not been set")
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
      Logger.warn("OAuth - state from session was empty")
    } 
    if (stateFromRequest.isEmpty) { 
      Logger.warn("OAuth - state from request was empty")
    } 
    if (stateFromSession != stateFromRequest) {
      Logger.warn(s"OAuth - state from request was $stateFromRequest but state from session was $stateFromSession")
    }


    // TODO: Parse response and capture access token
    case class GoogleTokenResponse(
      access_token: String,
      token_type: String,
      expires_in: String,
      id_token: Option[String],
      refresh_token: Option[String]
    )


    /**
     * Calls Google's OAuth2 token endpoint to swap a code for an auth_token
     */
    def authTokenFromCode(code:String):Ref[String] = {
      val ws = WS.url("https://accounts.google.com/o/oauth2/token").
        withHeaders("Accept" -> "application/json").
        post(Map[String, Seq[String]](
          "grant_type" -> Seq("authorization_code"),

          // Google uses this to validate this matches the redirect_uri from when authorization was requested
          "redirect_uri" -> Seq(routes.GooglePlusController.callback().absoluteURL()),

          "code" -> Seq(code),
          "client_id" -> clientKey.toSeq,
          "client_secret" -> secret.toSeq
      ))
      for {
        resp <- new RefFuture(ws)
        t <- (resp.json \ "access_token").asOpt[String]
      } yield t
    }
    
    /**
     * Given an authentication token, goes and looks up that user's details on GitHub.
     * These are filled into an "Interstitial Memory" -- details to remember during the display
     * of the confirmation page.
     */
    def userFromAuth(authToken:String) = {
      val ws = WS.url("https://www.googleapis.com/plus/v1/people/me").
                 withHeaders(
                   "Accept" -> "application/json",
                   "Authorization" -> ("Bearer " + authToken)
                 ).get()
      
      for (
        resp <- new RefFuture(ws);
        json = {
          println(resp.json)
          resp.json
        };
        id <- (resp.json \ "id").asOpt[String]
      ) yield {
        OAuthDetails(
          userRecord = UserRecord(
            service = GooglePlus.name,
            id = id,
            name = (json \ "displayName").asOpt[String],
            nickname = (json \ "displayName").asOpt[String],
            username = None,
            avatar = (json \ "image" \ "url").asOpt[String]
          ),
          raw = Some(json)
        )
      }
    }    
    
    val refMem = for (
      code <- Ref(request.getQueryString("code")) orIfNone AuthFailed("Google+ provided no code");
      authToken <- authTokenFromCode(code) orIfNone AuthFailed("Google+ did not provide an authorization token");
      mem <- userFromAuth(authToken) orIfNone AuthFailed("Google+ did not provide any user data for that login")
    ) yield mem
    
    PlayAuth.onAuthR(refMem)(request)
  }
  
}